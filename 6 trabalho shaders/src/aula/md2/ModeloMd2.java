package aula.md2;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.lwjgl.BufferUtils;

import aula.shaders.ShaderProgram;

// =====================================================================
// MODELO MD2 JA CARREGADO (versao com SHADER)
// Diferenca para o trabalho anterior:
//   antes: o Java calculava a pose interpolada todo quadro e mandava para o VBO
//   agora: TODAS as poses vao para o VBO uma vez so, e o VERTEX SHADER interpola
//
//   - INTERPOLAR as animacoes  -> atualizar(dt) escolhe frameA, frameB e t;
//                                 a conta "a + (b - a) * t" esta no vertexShader.txt
//   - TEXTURIZAR               -> desenhase() liga a textura; o fragmentShader.txt pinta
//   - EXECUTAR as animacoes    -> separaAnimacoes(), trocarAnimacao(), proximaAnimacao()
// =====================================================================
public class ModeloMd2 {
	public float frames[][];      // frames[f] = x,y,z de cada vertice na pose f
	public String nomesFrames[];  // nome de cada pose: "stand01", "run03"...
	public int indices[];         // 3 indices de vertice por triangulo
	public float uvs[];           // 3 pares (u,v) de textura por triangulo

	// animacao: nome -> {primeiro frame, ultimo frame}. Ex.: "run" -> {40, 45}
	public LinkedHashMap<String, int[]> animacoes = new LinkedHashMap<>();
	public String animacaoAtual;
	float tempo = 0;              // em frames: 2.4 = estamos 40% do caminho entre a pose 2 e a 3
	float fps = 9;                // poses por segundo (velocidade da animacao)

	// o que o shader precisa saber para interpolar
	int frameA, frameB;           // pose atual e proxima pose
	float t;                      // de 0 a 1: quanto ja andou de A para B

	public int textura = 0;       // id da textura no OpenGL
	int vboVertices;              // VBO com as posicoes de TODOS os frames
	int vboUV;                    // VBO com as coordenadas de textura
	long bytesPorFrame;           // tamanho de um frame dentro do vboVertices

	// ---------- EXECUTAR ANIMACOES: descobrir quais animacoes existem ----------
	// O md2 nao tem uma lista de animacoes. Elas aparecem pelo NOME dos frames:
	// "stand01", "stand02"... "run01", "run02"... Tirando os numeros do fim,
	// os frames com o mesmo nome formam uma animacao.
	void separaAnimacoes() {
		for (int f = 0; f < nomesFrames.length; f++) {
			String nome = nomesFrames[f].replaceAll("[0-9]+$", ""); // "run03" -> "run"
			if (animacoes.containsKey(nome)) {
				animacoes.get(nome)[1] = f;           // estende o fim da animacao
			} else {
				animacoes.put(nome, new int[] { f, f }); // primeira vez que aparece
			}
		}
		animacaoAtual = animacoes.keySet().iterator().next();
		System.out.println("Animacoes: " + animacoes.keySet());
	}

	// ---------- VBOs: dados guardados na placa de video ----------
	void criaVBOs() {
		// UV: a textura gruda sempre no mesmo lugar, entao manda uma vez so
		FloatBuffer uvBuffer = BufferUtils.createFloatBuffer(uvs.length);
		uvBuffer.put(uvs).flip();
		vboUV = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vboUV);
		glBufferData(GL_ARRAY_BUFFER, uvBuffer, GL_STATIC_DRAW);

		// posicoes: coloca os frames um depois do outro no MESMO VBO
		// [ frame 0 inteiro ][ frame 1 inteiro ][ frame 2 inteiro ] ...
		// cada frame tem 3 vertices por triangulo e 3 floats (x,y,z) por vertice
		int floatsPorFrame = indices.length * 3;
		bytesPorFrame = floatsPorFrame * 4L; // float = 4 bytes
		FloatBuffer todos = BufferUtils.createFloatBuffer(frames.length * floatsPorFrame);
		for (int f = 0; f < frames.length; f++) {
			for (int i = 0; i < indices.length; i++) {
				int v = indices[i] * 3;
				todos.put(frames[f][v]);     // x
				todos.put(frames[f][v + 1]); // y
				todos.put(frames[f][v + 2]); // z
			}
		}
		todos.flip();

		// STATIC: os dados nunca mudam, a animacao e feita no shader
		vboVertices = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vboVertices);
		glBufferData(GL_ARRAY_BUFFER, todos, GL_STATIC_DRAW);
		glBindBuffer(GL_ARRAY_BUFFER, 0);

		atualizar(0);
	}

	// ---------- EXECUTAR ANIMACOES: trocar qual esta rodando ----------
	public void trocarAnimacao(String nome) {
		animacaoAtual = nome;
		tempo = 0; // comeca do primeiro frame
	}

	// vai para a proxima (+1) ou anterior (-1) animacao da lista (setas do teclado)
	public void proximaAnimacao(int direcao) {
		ArrayList<String> nomes = new ArrayList<>(animacoes.keySet());
		int i = (nomes.indexOf(animacaoAtual) + direcao + nomes.size()) % nomes.size();
		trocarAnimacao(nomes.get(i));
	}

	// ---------- INTERPOLACAO: parte do Java ----------
	// Chamado todo quadro. dt = segundos desde o quadro anterior.
	// Aqui so decide ENTRE QUAIS poses estamos (frameA e frameB) e O QUANTO (t).
	// A conta da pose intermediaria, pose = poseA + (poseB - poseA) * t,
	// e feita pelo vertex shader (shaders/vertexShader.txt).
	public void atualizar(float dt) {
		int inicio = animacoes.get(animacaoAtual)[0];
		int fim = animacoes.get(animacaoAtual)[1];
		int quantidade = fim - inicio + 1;

		// avanca o tempo; o % faz voltar para o comeco quando chega no fim (loop)
		tempo = (tempo + dt * fps) % quantidade;

		frameA = inicio + (int) tempo;                       // pose atual
		frameB = inicio + ((int) tempo + 1) % quantidade;    // proxima pose
		t = tempo - (int) tempo;                             // parte quebrada: quanto ja andou de A para B
	}

	// ---------- DESENHO COM SHADER ----------
	// mesmo jeito do ObjModel.draw() do professor: glVertexAttribPointer + glDrawArrays
	public void desenhase(ShaderProgram shader) {
		glBindTexture(GL_TEXTURE_2D, textura);       // qual imagem usar (a skin)

		// manda o t para o shader (variavel "uniform float t")
		glUniform1f(glGetUniformLocation(shader.programID, "t"), t);

		// atributo 0 (poseA): le o vboVertices a partir do comeco do frameA
		// atributo 1 (poseB): le o MESMO vboVertices a partir do comeco do frameB
		// assim o shader recebe, para cada vertice, a posicao nas duas poses
		glBindBuffer(GL_ARRAY_BUFFER, vboVertices);
		glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, frameA * bytesPorFrame);
		glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, frameB * bytesPorFrame);
		glEnableVertexAttribArray(0);
		glEnableVertexAttribArray(1);

		// atributo 2 (textuv): coordenadas de textura, 2 floats (u,v) por vertice
		glBindBuffer(GL_ARRAY_BUFFER, vboUV);
		glVertexAttribPointer(2, 2, GL_FLOAT, false, 0, 0L);
		glEnableVertexAttribArray(2);

		glDrawArrays(GL_TRIANGLES, 0, indices.length); // desenha todos os triangulos

		glDisableVertexAttribArray(0);
		glDisableVertexAttribArray(1);
		glDisableVertexAttribArray(2);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
	}
}
