import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.PropertyResourceBundle;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import core2d.Linha2D;
import core2d.Ponto2D;
import core3d.LeitorObj;
import core3d.Mat4x4;
import core3d.Ponto3D;
import core3d.Triangulo3D;

public class MainCanvas extends JPanel implements Runnable{
	int W = 640;
	int H = 480;
	
	Thread runner;
	boolean ativo = true;
	int paintcounter = 0;
	
	BufferedImage imageBuffer;
	byte bufferDeVideo[];
	
	Random rand = new Random();
	
	byte memoriaPlacaVideo[];
	short paleta[][];
	
	int framecount = 0;
	int fps = 0;
	
	Font f = new Font("", Font.PLAIN, 30);
	Font fHud = new Font("", Font.PLAIN, 12);
	
	int clickX = 0;
	int clickY = 0;
	int mouseX = 0;
	int mouseY = 0;
	
	int pixelSize = 0;
	int Largura = 0;
	int Altura = 0;
	
	BufferedImage imgtmp = null;
	
	float posx = 00;
	float posy = 00;
	
	boolean LEFT = false;
	boolean RIGHT = false;
	boolean UP = false;
	boolean DOWN = false;
	
	float filtroR = 1;
	float filtroG = 1;
	float filtroB = 1;
	
	float q1x = 10,q1y = 100;
	float q2x = 10,q2y = 200;
	
	//ArrayList<Linha2D> linhas = new ArrayList<Linha2D>();
	ArrayList<Triangulo3D> listaDeTriangulos = new ArrayList<>();
	
	Ponto3D p0 = null;
	Ponto3D p1 = null;
	
	Ponto2D pC = new Ponto2D(320, 240);
	
	int eixoX = 0;
	int eixoY = 0;
	
	Mat4x4 projecao;
	Mat4x4 modelview;

	static final String PASTA_OBJ = "C:/Users/Micro/Downloads/obj_1";
	static final String ARQ_CARRUAGEM = "C:/Users/Micro/Downloads/Horse_and_Carriage_v1_L3.123c01e3954a-0422-487a-a2c3-78024c987f11/13915_Horse_and_Carriage_v1_l3.obj";
	
	public MainCanvas() {
		
		File f = new File("imgbmp.bmp");
		try {
			FileInputStream fin = new FileInputStream(f);

			byte todosodbytes[] = new byte[64000];
			int byteslidos = fin.read(todosodbytes);
			System.out.println("Bytes Lidos "+byteslidos);
			for(int i = 0; i < byteslidos;i++) {
				System.out.println(i+": "+todosodbytes[i]);
			}
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		
		setSize(640,480);
		setFocusable(true);
		
		Largura = 640;
		Altura = 480;
		
		pixelSize = 640*480;
		
		
//		try {
//			imgtmp = ImageIO.read(getClass().getResource("fundo.jpg"));
//			System.out.println(""+imgtmp.toString());
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
		
		imgtmp = loadImage("gato.jpg");
		
		imageBuffer = new BufferedImage(640,480, BufferedImage.TYPE_4BYTE_ABGR);
		//imageBuffer.getGraphics().drawImage(imgtmp, 0, 0, null);
		
		
		bufferDeVideo = ((DataBufferByte)imageBuffer.getRaster().getDataBuffer()).getData();
		
		System.out.println("Buffer SIZE "+bufferDeVideo.length );
		
		
		addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void keyReleased(KeyEvent e) {
				int key = e.getKeyCode();
				if (key == KeyEvent.VK_W) {
					UP = false;
				}
				if (key == KeyEvent.VK_S) {
					DOWN = false;
				}
				if (key == KeyEvent.VK_A) {
					LEFT = false;
				}
				if (key == KeyEvent.VK_D) {
					RIGHT = false;
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {
				int key = e.getKeyCode();
				// System.out.println("CLICO "+key);
				if (key == KeyEvent.VK_W) {
					UP = true;
				}
				if (key == KeyEvent.VK_S) {
					DOWN = true;
				}
				if (key == KeyEvent.VK_A) {
					LEFT = true;
				}
				if (key == KeyEvent.VK_D) {
					RIGHT = true;
				}
				// zoom
				if (key == KeyEvent.VK_Z) {
					focal = Math.max(100, focal * 0.8f);
				}
				if (key == KeyEvent.VK_X) {
					focal = Math.min(3000, focal * 1.25f);
				}
				if (key == KeyEvent.VK_Q || key == KeyEvent.VK_LEFT) {
					camYaw -= 5;
				}
				if (key == KeyEvent.VK_E || key == KeyEvent.VK_RIGHT) {
					camYaw += 5;
				}
				if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_4) {
					modoProjecao = key - KeyEvent.VK_0;
				}
			}

			
		});

		addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub
				clickX = e.getX();
				clickY = e.getY();
				
				if(e.getButton()==3) {
					eixoX = clickX;
					eixoY = clickY;
				}
				if(e.getButton()==1) {
					if(p0==null) {
						p0 = new Ponto3D(clickX,clickY,0);
					}else if(p1 == null) {
						p1 = new Ponto3D(clickX,clickY,0);
					}else {
						Ponto3D p2 = new Ponto3D(clickX,clickY,0);
						Triangulo3D tri = new Triangulo3D(p0, p1, p2);
						listaDeTriangulos.add(tri);
						p0 = null;
						p1 = null;
					}
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseClicked(MouseEvent e) {
				// TODO Auto-generated method stub

			}
		});

		addMouseMotionListener(new MouseMotionListener() {

			@Override
			public void mouseMoved(MouseEvent arg0) {
				// TODO Auto-generated method stub
				mouseX = arg0.getX();
				mouseY = arg0.getY();
			}

			@Override
			public void mouseDragged(MouseEvent arg0) {
				// TODO Auto-generated method stub

			}
		});
		
		modelview = new Mat4x4();
		projecao = new Mat4x4();
		atualizaCamera();

		criaCenario();
		
		System.out.println("Total de triangulos na cena: " + listaDeTriangulos.size());

	}
	
	static final float CHAO_Y = 380;

	private void criaCenario() {
		criaPlano(-80, -100, 800, 600, 50, CHAO_Y);

		listaDeTriangulos.addAll(LeitorObj.carregar(PASTA_OBJ + "/medieval house.obj", 200, 170, CHAO_Y, 200,  90)); // casa
		listaDeTriangulos.addAll(LeitorObj.carregar(PASTA_OBJ + "/Bench_LowRes.obj",    90, 400, CHAO_Y, 200, -90)); // banco

		// arvores
		listaDeTriangulos.addAll(LeitorObj.carregar("arvore.obj", 110,  70, CHAO_Y, 345));
		listaDeTriangulos.addAll(LeitorObj.carregar("arvore.obj", 130, 365, CHAO_Y, 330));
		listaDeTriangulos.addAll(LeitorObj.carregar("arvore.obj",  90, 150, CHAO_Y,  20));

		// galinhas
		listaDeTriangulos.addAll(LeitorObj.carregar("galinha.obj", 28, 290, CHAO_Y, 160,   45));
		listaDeTriangulos.addAll(LeitorObj.carregar("galinha.obj", 28, 320, CHAO_Y, 245, -120));
		listaDeTriangulos.addAll(LeitorObj.carregar("galinha.obj", 24, 265, CHAO_Y, 215,  -90));

		// espada na pedra
		listaDeTriangulos.addAll(LeitorObj.carregar("espada_pedra.obj", 75, 310, CHAO_Y, 310));

		// carruagem (arquivo com Z para cima)
		listaDeTriangulos.addAll(LeitorObj.carregar(ARQ_CARRUAGEM, 260, 560, CHAO_Y, 420, -105 - 90, true));
	}

	// chao em grade
	private void criaPlano(float x0, float z0, float largura, float profundidade, float passo, float y) {
		for (float x = x0; x < x0 + largura; x += passo) {
			for (float z = z0; z < z0 + profundidade; z += passo) {
				Ponto3D a = new Ponto3D(x, y, z);
				Ponto3D b = new Ponto3D(x + passo, y, z);
				Ponto3D c = new Ponto3D(x + passo, y, z + passo);
				Ponto3D d = new Ponto3D(x, y, z + passo);
				listaDeTriangulos.add(new Triangulo3D(a, b, c));
				listaDeTriangulos.add(new Triangulo3D(c, d, a));
			}
		}
	}

	private void drawImageToBuffer(BufferedImage image,int x,int y, float fr, float fg, float fb) {
		byte[] imgBuffer = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
		
		
		int iw = image.getWidth();
		int ih = image.getHeight();
		
		for(int yi = 0; yi < ih; yi++) {
			for(int xi = 0; xi < iw; xi++) {
				int pixi = yi*iw*4 + xi*4;
				int pixb = (yi+y)*W*4 + (xi+x)*4;
				bufferDeVideo[pixb] = imgBuffer[pixi];
			
				
				
				int b = (imgBuffer[pixi+1]&0xff);
				int g =	(imgBuffer[pixi+2]&0xff);
				int r = (imgBuffer[pixi+3]&0xff);
				
				b = (int)(b*fb);
				g = (int)(g*fg);
				r = (int)(r*fr);
//				
				b = Math.min(255, b);
				g = Math.min(255, g);
				r = Math.min(255, r);
				
				bufferDeVideo[pixb+1] = (byte)(b&0xff);
				bufferDeVideo[pixb+2] = (byte)(g&0xff);
				bufferDeVideo[pixb+3] = (byte)(r&0xff);
			}
		}
	}
	static final float ALTURA_OLHO = 50;
	static final float VEL_CAMERA = 150;
	float camX = 320;
	float camY = CHAO_Y - ALTURA_OLHO;
	float camZ = -150;
	float camYaw = 0;
	float focal = 500;
	int modoProjecao = 4; // 1 paralela, 2 cavaleira, 3 gabinete, 4 perspectiva

	private void atualizaCamera() {
		Mat4x4 t = new Mat4x4();
		t.setTranslate(-camX, -camY, -camZ);
		Mat4x4 r = new Mat4x4();
		r.setRotateY(camYaw);
		modelview = modelview.multiplicaMatrizes(r, t);

		if (modoProjecao == 4) {
			projecao.setPerspectiveCamera(focal, 320, 240);
		} else {
			Mat4x4 p = new Mat4x4();
			if (modoProjecao == 1) p.setParalelProjection();
			if (modoProjecao == 2) p.setObliqueProjection(1, 45);
			if (modoProjecao == 3) p.setObliqueProjection(0.5f, 30);
			Mat4x4 centro = new Mat4x4();
			centro.setTranslate(320, 240, 0);
			Mat4x4 zoom = new Mat4x4();
			zoom.setSacale(focal / 500, focal / 500, focal / 500);
			Mat4x4 plano = new Mat4x4();
			plano.setTranslate(0, 0, -300);
			projecao = projecao.multiplicaMatrizes(centro, projecao.multiplicaMatrizes(p, projecao.multiplicaMatrizes(zoom, plano)));
		}
	}
	
	int timer = 0;
	public void simulaMundo(long diftime){
		
		float difS = diftime/1000.0f;
		float vel = 50;
		
		timer+=diftime;
		
		float passo = VEL_CAMERA * difS;
		float rad = camYaw * 0.017453f;
		float frenteX = (float)Math.sin(rad), frenteZ = (float)Math.cos(rad);
		float ladoX = (float)Math.cos(rad), ladoZ = -(float)Math.sin(rad);
		if (UP)    { camX += frenteX * passo; camZ += frenteZ * passo; }
		if (DOWN)  { camX -= frenteX * passo; camZ -= frenteZ * passo; }
		if (RIGHT) { camX += ladoX * passo;   camZ += ladoZ * passo; }
		if (LEFT)  { camX -= ladoX * passo;   camZ -= ladoZ * passo; }
		
		atualizaCamera();
		
//		for (int i = 0; i < listaDeTriangulos.size(); i++) {
//			Triangulo3D tri = listaDeTriangulos.get(i);
//			if (UP) {
//				tri.translacao(0, -1,0);
//			}
//			if (DOWN) {
//				tri.translacao(0, +1,0);
//			}
//			if (LEFT) {
//				tri.translacao(-1, 0,0);
//			}
//			if (RIGHT) {
//				tri.translacao(+1, 0,0);
//			}
//		}
		
	}
	
	@Override
	public void paint(Graphics g) {
		
		for(int i = 0; i < bufferDeVideo.length; i++) {
			bufferDeVideo[i] = 0;
		}
		
		
		g.setFont(f);
		
		g.setColor(Color.white);
		g.fillRect(0, 0, 800, 600);
		
		g.setColor(Color.blue);
		g.fillRect(eixoX-2, eixoY-2, 5, 5);

//		g.setColor(Color.green);
//		g.drawRect(50, 50, 700, 500);

//		g.setColor(Color.black);
//		for(int i = 0; i < listaDeLinhas.size();i++) {
//			listaDeLinhas.get(i).desenhase((Graphics2D)g);
//		}

		g.setColor(Color.black);
		for (int i = 0; i < listaDeTriangulos.size(); i++) {
			Triangulo3D tri = listaDeTriangulos.get(i);
			tri.desenhase((Graphics2D) g,modelview,projecao);
		}
		
		g.setColor(Color.red);
		if(p0!=null) {
			if(p1!=null) {
				g.drawLine((int)p0.x,(int)p0.y,(int)p1.x,(int)p1.y);
				g.drawLine((int)p0.x,(int)p0.y,(int)mouseX,(int)mouseY);
				g.drawLine((int)p1.x,(int)p1.y,(int)mouseX,(int)mouseY);
			}else {
				g.drawLine((int)p0.x,(int)p0.y,(int)mouseX,(int)mouseY);
			}
		}

		
		g.setColor(Color.black);
		g.drawString("FPS "+fps+" mouse: "+mouseX+","+mouseY, 10, 25);
		g.setFont(fHud);
		g.drawString("WASD anda | Q/E ou setas gira | Z/X zoom | 1-4 projecao", 10, 470);
		g.drawString(String.format("cam x=%.0f z=%.0f yaw=%.0f focal=%.0f proj=%d", camX, camZ, camYaw, focal, modoProjecao), 10, 452);
	}
	
	public void desenhaLinhaHorizontal(int x, int y,int w) {
		int pospix = y*(W*4)+x*4;
		
		for(int i = 0; i < w;i++) {
			
			bufferDeVideo[pospix] = (byte)255;
			bufferDeVideo[pospix+1] = (byte)0;
			bufferDeVideo[pospix+2] = (byte)0;
			bufferDeVideo[pospix+3] = (byte)0;
			pospix+=4;
		}
	}
	
	public void desenhaLinhaVertical(int x, int y,int h) {
		int pospix = y*(W*4)+x*4;
		
		for(int i = 0; i < h;i++) {
			
			bufferDeVideo[pospix] = (byte)255;
			bufferDeVideo[pospix+1] = (byte)0;
			bufferDeVideo[pospix+2] = (byte)0;
			bufferDeVideo[pospix+3] = (byte)255;
			pospix+=(W*4);
		}
	}
	
	public void desenhaPixel(int x, int y,int r,int g,int b) {
		int pospix = y*(W*4)+x*4;
			
		bufferDeVideo[pospix] = (byte)255;
		bufferDeVideo[pospix+1] = (byte)(b&0xff);
		bufferDeVideo[pospix+2] = (byte)(g&0xff);
		bufferDeVideo[pospix+3] = (byte)(r&0xff);
	
	}
	
	public void start(){
		runner = new Thread(this);
		runner.start();
	}
	

	
	
	@Override
	public void run() {
		long time = System.currentTimeMillis();
		long segundo = time/1000;
		long diftime = 0;
		while(ativo){
			simulaMundo(diftime);
			paintImmediately(0, 0, 640, 480);
			paintcounter+=100;
			
			try {
				Thread.sleep(0);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			long newtime = System.currentTimeMillis();
			long novoSegundo = newtime/1000;
			diftime = System.currentTimeMillis() - time;
			time = System.currentTimeMillis();
			framecount++;
			if(novoSegundo!=segundo) {	
				fps = framecount;
				framecount = 0;
				segundo = novoSegundo;
			}
		}
	}
	
	
	public BufferedImage loadImage(String filename) {
		try {
			imgtmp = ImageIO.read(new File(filename));
			
			BufferedImage imgout = new BufferedImage(imgtmp.getWidth(), imgtmp.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
			
			imgout.getGraphics().drawImage(imgtmp, 0, 0, null);
			
			imgtmp = null;
			
			return imgout;
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
	}
}
