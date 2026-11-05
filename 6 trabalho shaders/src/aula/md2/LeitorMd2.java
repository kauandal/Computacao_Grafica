package aula.md2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;

// =====================================================================
// LEITOR DE ARQUIVO MD2 (formato de modelo animado do Quake 2)
//
// Diferente do .obj (que e texto), o .md2 e BINARIO: sao bytes em sequencia.
// Ele tem 4 partes que interessam:
//   1) cabecalho  -> quantos vertices, triangulos, frames... e ONDE (offset) cada parte comeca
//   2) coordenadas de textura (s, t) -> onde cada ponto cai na imagem da skin
//   3) triangulos -> quais 3 vertices e quais 3 coordenadas de textura formam cada triangulo
//   4) frames     -> a posicao de TODOS os vertices em cada quadro da animacao
//
// A animacao por keyframe e isso: o arquivo guarda o boneco inteiro em varias poses
// (frames). Para animar, a gente vai trocando de pose e interpolando entre elas.
// =====================================================================
public class LeitorMd2 {

	// Mesmo jeito do LeitorObj do CG_4_1:
	// tamanho = maior dimensao do modelo, (cx, cz) = centro no chao, cy = altura da base, rotY = giro em graus
	public static ModeloMd2 carregar(String caminho, float tamanho, float cx, float cy, float cz, float rotY) {
		ModeloMd2 modelo = new ModeloMd2();

		try {
			// le o arquivo inteiro para a memoria. LITTLE_ENDIAN = ordem dos bytes usada pelo Quake 2
			ByteBuffer buf = ByteBuffer.wrap(Files.readAllBytes(Paths.get(caminho))).order(ByteOrder.LITTLE_ENDIAN);

			// ---------- 1) CABECALHO: 17 numeros inteiros, um atras do outro ----------
			int ident = buf.getInt();
			int versao = buf.getInt();
			if (ident != 0x32504449 || versao != 8) { // todo md2 comeca com "IDP2" e versao 8
				throw new IOException("nao e um arquivo md2");
			}
			int skinW = buf.getInt();          // largura da imagem da textura
			int skinH = buf.getInt();          // altura da imagem da textura
			buf.getInt(); // tamanho do frame (nao usado)
			buf.getInt(); // numero de skins (nao usado)
			int numVertices = buf.getInt();    // vertices em cada frame
			int numST = buf.getInt();          // quantidade de coordenadas de textura
			int numTriangulos = buf.getInt();
			buf.getInt(); // numero de comandos gl (nao usado)
			int numFrames = buf.getInt();      // quantos quadros de animacao existem
			buf.getInt(); // offset das skins (nao usado)
			int ofsST = buf.getInt();          // byte onde comecam as coordenadas de textura
			int ofsTriangulos = buf.getInt();  // byte onde comecam os triangulos
			int ofsFrames = buf.getInt();      // byte onde comecam os frames

			// ---------- 2) COORDENADAS DE TEXTURA ----------
			// vem em pixels da imagem (ex.: 128, 64). O OpenGL usa de 0 a 1,
			// entao divide pela largura/altura da imagem.
			float st[] = new float[numST * 2];
			buf.position(ofsST);
			for (int i = 0; i < numST; i++) {
				st[i * 2] = buf.getShort() / (float) skinW;
				st[i * 2 + 1] = buf.getShort() / (float) skinH;
			}

			// ---------- 3) TRIANGULOS ----------
			// cada triangulo tem 3 numeros que apontam para vertices
			// e 3 numeros que apontam para coordenadas de textura
			modelo.indices = new int[numTriangulos * 3];
			modelo.uvs = new float[numTriangulos * 3 * 2];
			buf.position(ofsTriangulos);
			for (int i = 0; i < numTriangulos; i++) {
				for (int k = 0; k < 3; k++) {
					modelo.indices[i * 3 + k] = buf.getShort();
				}
				for (int k = 0; k < 3; k++) {
					int s = buf.getShort();
					modelo.uvs[(i * 3 + k) * 2] = st[s * 2];
					modelo.uvs[(i * 3 + k) * 2 + 1] = st[s * 2 + 1];
				}
			}

			// ---------- 4) FRAMES (as poses da animacao) ----------
			// cada frame tem: escala (3 floats), translacao (3 floats), nome (16 letras, ex. "run03")
			// e depois todos os vertices. Para economizar espaco, cada coordenada e 1 byte (0 a 255);
			// a posicao real e: byte * escala + translacao
			modelo.frames = new float[numFrames][numVertices * 3];
			modelo.nomesFrames = new String[numFrames];
			buf.position(ofsFrames);
			for (int f = 0; f < numFrames; f++) {
				float escala[] = { buf.getFloat(), buf.getFloat(), buf.getFloat() };
				float translacao[] = { buf.getFloat(), buf.getFloat(), buf.getFloat() };
				byte nome[] = new byte[16];
				buf.get(nome);
				modelo.nomesFrames[f] = new String(nome).replace("\0", " ").trim();
				for (int v = 0; v < numVertices; v++) {
					// "& 0xFF" transforma o byte em numero de 0 a 255 (em Java o byte tem sinal)
					float x = (buf.get() & 0xFF) * escala[0] + translacao[0];
					float y = (buf.get() & 0xFF) * escala[1] + translacao[1];
					float z = (buf.get() & 0xFF) * escala[2] + translacao[2];
					buf.get(); // indice da normal (nao usado)

					// no md2 o eixo Z aponta para cima; no OpenGL e o Y.
					// entao troca: (x, y, z) -> (x, z, -y)
					modelo.frames[f][v * 3] = x;
					modelo.frames[f][v * 3 + 1] = z;
					modelo.frames[f][v * 3 + 2] = -y;
				}
			}

			System.out.println("MD2 " + caminho + ": " + numVertices + " vertices, " + numTriangulos + " triangulos, " + numFrames + " frames");
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		// deixa o modelo do tamanho e na posicao que a gente quer
		if (tamanho > 0) {
			normaliza(modelo.frames, tamanho, cx, cy, cz, rotY);
		}

		modelo.separaAnimacoes(); // agrupa os frames em animacoes (stand, run, attack...)
		modelo.criaVBOs();        // manda os dados para a placa de video
		return modelo;
	}

	// Mesma ideia do LeitorObj do CG_4_1:
	// acha o tamanho e o centro do modelo (olhando o frame 0), escala para "tamanho",
	// coloca a base no chao (cy), gira rotY graus e posiciona em (cx, cz).
	// Aplica a MESMA transformacao em todos os frames, senao a animacao "pularia".
	private static void normaliza(float frames[][], float tamanho, float cx, float cy, float cz, float rotY) {
		// caixa envolvente (menor e maior x, y, z)
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		float f0[] = frames[0];
		for (int i = 0; i < f0.length; i += 3) {
			minX = Math.min(minX, f0[i]);     maxX = Math.max(maxX, f0[i]);
			minY = Math.min(minY, f0[i + 1]); maxY = Math.max(maxY, f0[i + 1]);
			minZ = Math.min(minZ, f0[i + 2]); maxZ = Math.max(maxZ, f0[i + 2]);
		}
		float maior = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
		float s = maior > 0 ? tamanho / maior : 1;          // fator de escala
		float mx = (minX + maxX) / 2, mz = (minZ + maxZ) / 2; // centro
		float rad = rotY * 0.017453f;                        // graus -> radianos
		float sin = (float) Math.sin(rad), cos = (float) Math.cos(rad);

		for (float f[] : frames) {
			for (int i = 0; i < f.length; i += 3) {
				float x = (f[i] - mx) * s;           // centraliza e escala
				float z = (f[i + 2] - mz) * s;
				f[i] = x * cos + z * sin + cx;       // rotacao em Y e posicao
				f[i + 1] = (f[i + 1] - minY) * s + cy; // base no chao
				f[i + 2] = -x * sin + z * cos + cz;
			}
		}
	}
}
