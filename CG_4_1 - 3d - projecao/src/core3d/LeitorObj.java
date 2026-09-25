package core3d;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

// le so "v" e o primeiro valor de cada item do "f"
public class LeitorObj {

	public static ArrayList<Triangulo3D> carregar(String caminho) {
		return carregar(caminho, 0, 0, 0, 0);
	}

	public static ArrayList<Triangulo3D> carregar(String caminho, float tamanho, float cx, float cy, float cz) {
		return carregar(caminho, tamanho, cx, cy, cz, 0);
	}

	public static ArrayList<Triangulo3D> carregar(String caminho, float tamanho, float cx, float cy, float cz, float rotY) {
		return carregar(caminho, tamanho, cx, cy, cz, rotY, false);
	}

	// tamanho = maior dimensao, (cx, cz) = centro, cy = base, rotY em graus
	public static ArrayList<Triangulo3D> carregar(String caminho, float tamanho, float cx, float cy, float cz, float rotY, boolean zParaCima) {
		ArrayList<Ponto3D> vertices = new ArrayList<>();
		ArrayList<int[]> faces = new ArrayList<>();

		try (BufferedReader br = new BufferedReader(new FileReader(caminho))) {
			String linha;
			while ((linha = br.readLine()) != null) {
				String[] partes = linha.trim().split("\\s+");

				if (partes[0].equals("v")) {
					float x = Float.parseFloat(partes[1]);
					float y = Float.parseFloat(partes[2]);
					float z = Float.parseFloat(partes[3]);
					if (zParaCima) {
						vertices.add(new Ponto3D(x, z, -y));
					} else {
						vertices.add(new Ponto3D(x, y, z));
					}
				} else if (partes[0].equals("f")) {
					int n = partes.length - 1;
					int[] face = new int[n];
					for (int i = 0; i < n; i++) {
						int idx = Integer.parseInt(partes[i + 1].split("/")[0]);
						if (idx < 0) {
							idx = vertices.size() + idx + 1;
						}
						face[i] = idx - 1;
					}
					faces.add(face);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (tamanho > 0 && vertices.size() > 0) {
			normaliza(vertices, tamanho, cx, cy, cz, rotY);
		}

		ArrayList<Triangulo3D> triangulos = new ArrayList<>();
		for (int[] face : faces) {
			for (int i = 1; i < face.length - 1; i++) {
				triangulos.add(new Triangulo3D(vertices.get(face[0]), vertices.get(face[i]), vertices.get(face[i + 1])));
			}
		}

		System.out.println("OBJ " + caminho + ": " + vertices.size() + " vertices, " + triangulos.size() + " triangulos");
		return triangulos;
	}

	private static void normaliza(ArrayList<Ponto3D> vertices, float tamanho, float cx, float cy, float cz, float rotY) {
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
		for (Ponto3D p : vertices) {
			minX = Math.min(minX, p.x); maxX = Math.max(maxX, p.x);
			minY = Math.min(minY, p.y); maxY = Math.max(maxY, p.y);
			minZ = Math.min(minZ, p.z); maxZ = Math.max(maxZ, p.z);
		}
		float maior = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ));
		float s = maior > 0 ? tamanho / maior : 1;
		float mx = (minX + maxX) / 2, mz = (minZ + maxZ) / 2;
		float rad = rotY * 0.017453f;
		float sin = (float)Math.sin(rad), cos = (float)Math.cos(rad);

		for (Ponto3D p : vertices) {
			float x = (p.x - mx) * s;
			float z = (p.z - mz) * s;
			p.x = x * cos + z * sin + cx;
			p.z = -x * sin + z * cos + cz;
			p.y = -(p.y - minY) * s + cy;
		}
	}
}
