package aula.shaders;

// Mesmo jeito do StaticShader do professor: diz quais arquivos usar
// e qual numero (atributo) cada variavel "in" do vertex shader recebe.
public class Md2Shader extends ShaderProgram {

	private static final String VERTEX_FILE = "./shaders/vertexShader.txt";
	private static final String FRAGMENT_FILE = "./shaders/fragmentShader.txt";

	public Md2Shader() {
		super(VERTEX_FILE, FRAGMENT_FILE);
	}

	@Override
	protected void bindAttributes() {
		super.bindAttribute(0, "poseA");  // vertice no frame atual
		super.bindAttribute(1, "poseB");  // vertice no proximo frame
		super.bindAttribute(2, "textuv"); // coordenada de textura
	}
}
