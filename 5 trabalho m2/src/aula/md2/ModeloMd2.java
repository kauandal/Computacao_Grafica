package aula.md2;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.lwjgl.BufferUtils;

// =====================================================================
// MODELO MD2 JA CARREGADO
// Guarda as poses (frames), os triangulos e a textura,
// e faz as 3 coisas do trabalho:
//   - INTERPOLAR as animacoes  -> atualizar(dt)
//   - TEXTURIZAR               -> desenhase() usa a textura + as UVs
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

	public int textura = 0;       // id da textura no OpenGL
	int vboVertices;              // VBO com as posicoes (muda todo quadro)
	int vboUV;                    // VBO com as coordenadas de textura (nunca muda)
	FloatBuffer vertices;

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
		// UV: a textura gruda sempre no mesmo lugar, entao manda uma vez so (STATIC)
		FloatBuffer uvBuffer = BufferUtils.createFloatBuffer(uvs.length);
		uvBuffer.put(uvs).flip();
		vboUV = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vboUV);
		glBufferData(GL_ARRAY_BUFFER, uvBuffer, GL_STATIC_DRAW);

		// posicoes: mudam todo quadro por causa da interpolacao (DYNAMIC).
		// aqui so reserva o espaco; quem preenche e o atualizar()
		vertices = BufferUtils.createFloatBuffer(indices.length * 3);
		vboVertices = glGenBuffers();
		glBindBuffer(GL_ARRAY_BUFFER, vboVertices);
		glBufferData(GL_ARRAY_BUFFER, vertices.capacity() * 4L, GL_DYNAMIC_DRAW);
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

	// ---------- INTERPOLACAO (a parte principal do trabalho) ----------
	// Chamado todo quadro. dt = segundos desde o quadro anterior.
	// O arquivo so tem algumas poses; se a gente pulasse de uma para outra o boneco
	// ficaria "travando". Entao calculamos poses intermediarias:
	//   pose = poseA + (poseB - poseA) * t     (t vai de 0 ate 1)
	// t = 0 -> igual a poseA, t = 0.5 -> metade do caminho, t = 1 -> igual a poseB
	public void atualizar(float dt) {
		int inicio = animacoes.get(animacaoAtual)[0];
		int fim = animacoes.get(animacaoAtual)[1];
		int quantidade = fim - inicio + 1;

		// avanca o tempo; o % faz voltar para o comeco quando chega no fim (loop)
		tempo = (tempo + dt * fps) % quantidade;

		int frameA = inicio + (int) tempo;                       // pose atual
		int frameB = inicio + ((int) tempo + 1) % quantidade;    // proxima pose
		float t = tempo - (int) tempo;                           // parte quebrada: quanto ja andou de A para B

		float a[] = frames[frameA];
		float b[] = frames[frameB];

		// para cada vertice de cada triangulo, calcula a posicao interpolada
		vertices.clear();
		for (int i = 0; i < indices.length; i++) {
			int v = indices[i] * 3;
			vertices.put(a[v] + (b[v] - a[v]) * t);             // x
			vertices.put(a[v + 1] + (b[v + 1] - a[v + 1]) * t); // y
			vertices.put(a[v + 2] + (b[v + 2] - a[v + 2]) * t); // z
		}
		vertices.flip();

		// manda as posicoes novas para o VBO na placa de video
		glBindBuffer(GL_ARRAY_BUFFER, vboVertices);
		glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
		glBindBuffer(GL_ARRAY_BUFFER, 0);
	}

	// ---------- DESENHO COM TEXTURA ----------
	// mesmo jeito do ObjModel.draw() do professor: liga os VBOs e chama glDrawArrays
	public void desenhase() {
		glEnable(GL_TEXTURE_2D);
		glBindTexture(GL_TEXTURE_2D, textura);       // qual imagem usar (a skin)

		glEnableClientState(GL_VERTEX_ARRAY);
		glBindBuffer(GL_ARRAY_BUFFER, vboVertices);
		glVertexPointer(3, GL_FLOAT, 0, 0L);         // posicoes: 3 floats por vertice

		glEnableClientState(GL_TEXTURE_COORD_ARRAY);
		glBindBuffer(GL_ARRAY_BUFFER, vboUV);
		glTexCoordPointer(2, GL_FLOAT, 0, 0L);       // textura: 2 floats (u,v) por vertice

		glDrawArrays(GL_TRIANGLES, 0, indices.length); // desenha todos os triangulos

		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glDisableClientState(GL_TEXTURE_COORD_ARRAY);
		glDisableClientState(GL_VERTEX_ARRAY);
		glDisable(GL_TEXTURE_2D);
	}
}
