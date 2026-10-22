package aula;

import org.lwjgl.opengl.GL;

import aula.md2.LeitorMd2;
import aula.md2.ModeloMd2;
import aula.util.TextureLoader;

import java.io.File;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

// =====================================================================
// PROGRAMA PRINCIPAL
// Mesma organizacao do MainCanvas do CG_4_1:
//   criaCenario()    -> carrega os modelos
//   simulaMundo(dt)  -> move a camera e avanca as animacoes
//   atualizaCamera() -> monta as matrizes de projecao e da camera
//   laco principal   -> desenha tudo, quadro a quadro
// =====================================================================
public class Main {

	static long window;

	static ArrayList<ModeloMd2> listaDeModelos = new ArrayList<>();
	static ModeloMd2 ogro; // o que troca de animacao pelas setas

	// camera (igual ao CG_4_1)
	static final float CHAO_Y = 0;
	static final float ALTURA_OLHO = 1.7f;
	static final float VEL_CAMERA = 3;     // unidades por segundo
	static float camX = 0;
	static float camY = CHAO_Y + ALTURA_OLHO;
	static float camZ = 6;
	static float camYaw = 0;               // para onde a camera olha, em graus
	static float focal = 500;              // zoom
	static int modoProjecao = 4; // 1 paralela, 2 cavaleira, 3 gabinete, 4 perspectiva

	public static void main(String[] args) throws Exception {
		// ---------- abre a janela e liga o OpenGL (igual ao codigo base do professor) ----------
		if (!glfwInit())
			throw new IllegalStateException("GLFW nao iniciou");

		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 2);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
		window = glfwCreateWindow(1024, 768, "MD2", 0, 0);
		if (window == 0)
			throw new IllegalStateException("Falha ao criar janela");
		glfwMakeContextCurrent(window);
		glfwSwapInterval(1);
		GL.createCapabilities();
		glEnable(GL_DEPTH_TEST);   // o que esta na frente esconde o que esta atras
		glActiveTexture(GL_TEXTURE0);
		glClearColor(.5f, .5f, .5f, 1);

		// ---------- teclado: teclas que funcionam ao APERTAR ----------
		glfwSetKeyCallback(window, (w, key, scancode, action, mods) -> {
			if (action == GLFW_RELEASE)
				return;
			// zoom
			if (key == GLFW_KEY_Z) {
				focal = Math.max(100, focal * 0.8f);
			}
			if (key == GLFW_KEY_X) {
				focal = Math.min(3000, focal * 1.25f);
			}
			// girar a camera
			if (key == GLFW_KEY_Q) {
				camYaw -= 5;
			}
			if (key == GLFW_KEY_E) {
				camYaw += 5;
			}
			// trocar a projecao
			if (key >= GLFW_KEY_1 && key <= GLFW_KEY_4) {
				modoProjecao = key - GLFW_KEY_0;
			}
			// EXECUTAR ANIMACOES: as setas trocam a animacao do ogro do meio
			if (key == GLFW_KEY_RIGHT && action == GLFW_PRESS) {
				ogro.proximaAnimacao(1);
			}
			if (key == GLFW_KEY_LEFT && action == GLFW_PRESS) {
				ogro.proximaAnimacao(-1);
			}
			if (key == GLFW_KEY_ESCAPE) {
				glfwSetWindowShouldClose(w, true);
			}
		});

		try {
			criaCenario();

			// ---------- LACO PRINCIPAL: roda uma vez por quadro (~60 vezes por segundo) ----------
			double ultimo = glfwGetTime();
			int framecount = 0, fps = 0;
			double segundo = ultimo;
			while (!glfwWindowShouldClose(window)) {
				// dt = quanto tempo passou desde o quadro anterior (em segundos).
				// usar dt faz a animacao ter a mesma velocidade em qualquer computador
				double agora = glfwGetTime();
				float dt = (float) (agora - ultimo);
				ultimo = agora;

				// contador de FPS
				framecount++;
				if (agora - segundo >= 1) {
					fps = framecount;
					framecount = 0;
					segundo = agora;
				}

				int[] w = new int[1], h = new int[1];
				glfwGetFramebufferSize(window, w, h);
				if (h[0] == 0) {
					glfwPollEvents();
					continue;
				}

				simulaMundo(dt); // move camera + avanca/interpola as animacoes

				glViewport(0, 0, w[0], h[0]);
				glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // limpa a tela
				atualizaCamera((float) w[0] / h[0]);

				// segurando TAB desenha so as linhas (wireframe)
				glPolygonMode(GL_FRONT_AND_BACK, glfwGetKey(window, GLFW_KEY_TAB) == GLFW_PRESS ? GL_LINE : GL_FILL);

				// desenha a cena: chao e depois cada modelo
				desenhaChao(-10, -10, 20, 20, 1, CHAO_Y);
				glColor3f(1, 1, 1);
				for (int i = 0; i < listaDeModelos.size(); i++) {
					listaDeModelos.get(i).desenhase();
				}

				// informacoes no titulo da janela
				glfwSetWindowTitle(window, String.format("MD2  |  FPS %d  |  animacao: %s (setas)  |  cam x=%.1f z=%.1f yaw=%.0f focal=%.0f proj=%d",
						fps, ogro.animacaoAtual, camX, camZ, camYaw, focal, modoProjecao));

				glfwSwapBuffers(window); // mostra o quadro desenhado
				glfwPollEvents();        // le teclado/mouse
			}
		} finally {
			glfwDestroyWindow(window);
			glfwTerminate();
		}
	}

	// ---------- CENARIO: carrega a textura e os modelos ----------
	static void criaCenario() throws Exception {
		// TEXTURIZAR: le a imagem da skin e envia para o OpenGL (TextureLoader do professor)
		int skin = TextureLoader.loadTexture(ImageIO.read(new File("md2/grok.jpg")));

		// LeitorMd2.carregar(arquivo, tamanho, x, y, z, rotY)  - igual ao LeitorObj do CG_4_1
		// no md2 o modelo olha para +X; rotY = -90 vira ele para +Z (para a camera)
		ogro = LeitorMd2.carregar("md2/ogro.md2", 2, 0, CHAO_Y, 0, -90);
		ogro.textura = skin;
		listaDeModelos.add(ogro);

		// mais dois ogros, cada um com uma animacao diferente
		ModeloMd2 ogro2 = LeitorMd2.carregar("md2/ogro.md2", 2, -3, CHAO_Y, -2, -60);
		ogro2.textura = skin;
		ogro2.trocarAnimacao("run");
		listaDeModelos.add(ogro2);

		ModeloMd2 ogro3 = LeitorMd2.carregar("md2/ogro.md2", 2, 3, CHAO_Y, -2, -120);
		ogro3.textura = skin;
		ogro3.trocarAnimacao("attack");
		listaDeModelos.add(ogro3);
	}

	// ---------- SIMULACAO: roda todo quadro ----------
	static void simulaMundo(float dt) {
		// WASD anda na direcao em que a camera olha (igual ao CG_4_1)
		float passo = VEL_CAMERA * dt;
		float rad = camYaw * 0.017453f;
		float frenteX = (float) Math.sin(rad), frenteZ = -(float) Math.cos(rad);
		float ladoX = (float) Math.cos(rad), ladoZ = (float) Math.sin(rad);
		if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) { camX += frenteX * passo; camZ += frenteZ * passo; }
		if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) { camX -= frenteX * passo; camZ -= frenteZ * passo; }
		if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) { camX += ladoX * passo;   camZ += ladoZ * passo; }
		if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) { camX -= ladoX * passo;   camZ -= ladoZ * passo; }

		// EXECUTAR ANIMACOES: cada modelo avanca o tempo e interpola a pose
		for (int i = 0; i < listaDeModelos.size(); i++) {
			listaDeModelos.get(i).atualizar(dt);
		}
	}

	// ---------- CAMERA E PROJECAO ----------
	static void atualizaCamera(float aspect) {
		// matriz de PROJECAO: como o 3D vira 2D na tela
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		if (modoProjecao == 4) {
			// perspectiva: o que esta longe fica menor
			float perto = 0.1f;
			float meiaAltura = perto * 240 / focal;
			glFrustum(-meiaAltura * aspect, meiaAltura * aspect, -meiaAltura, meiaAltura, perto, 200);
		} else {
			// paralela: tamanho nao muda com a distancia
			float e = 3 * 500 / focal;
			glOrtho(-e * aspect, e * aspect, -e, e, -200, 200);
			if (modoProjecao == 2) obliqua(1, 45);     // cavaleira
			if (modoProjecao == 3) obliqua(0.5f, 30);  // gabinete
		}

		// matriz da CAMERA: gira e move o mundo ao contrario da camera
		// camera: R(yaw) * T(-camera)
		glMatrixMode(GL_MODELVIEW);
		glLoadIdentity();
		glRotatef(camYaw, 0, 1, 0);
		glTranslatef(-camX, -camY, -camZ);
	}

	// projecao obliqua (mesma matriz do CG_4_1):
	// x' = x + alpha*cos(theta)*z, y' = y + alpha*sin(theta)*z
	// (profundidade medida a partir de um plano 5 unidades a frente da camera)
	static void obliqua(float alpha, float theta) {
		float rad = theta * 0.017453f;
		float m[] = {
				1, 0, 0, 0,
				0, 1, 0, 0,
				alpha * (float) Math.cos(rad), alpha * (float) Math.sin(rad), 1, 0,
				0, 0, 0, 1 }; // OpenGL guarda a matriz por coluna
		glMultMatrixf(m);
		glTranslatef(0, 0, 5);
	}

	// chao em grade (linhas em X e em Z)
	static void desenhaChao(float x0, float z0, float largura, float profundidade, float passo, float y) {
		glColor3f(0.2f, 0.2f, 0.2f);
		glBegin(GL_LINES);
		for (float x = x0; x <= x0 + largura; x += passo) {
			glVertex3f(x, y, z0);
			glVertex3f(x, y, z0 + profundidade);
		}
		for (float z = z0; z <= z0 + profundidade; z += passo) {
			glVertex3f(x0, y, z);
			glVertex3f(x0 + largura, y, z);
		}
		glEnd();
	}
}
