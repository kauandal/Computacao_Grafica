# Trabalho: MD2 com shaders

Continuação do trabalho M2 (mesma estrutura e mesma cena), agora usando a parte de shaders do projeto `Opengl06_SHADERS_final`. O modelo MD2 é desenhado com shader e a interpolação entre os frames é feita no vertex shader.

Requisitos: JDK 17+ e placa com OpenGL 3.3. As bibliotecas do LWJGL (Windows) estão em `lib/`.

## Executar

No Eclipse: File > Import > Existing Projects into Workspace, escolher esta pasta e executar `aula.Main`.

Pelo terminal, na pasta do projeto:

```bash
javac -d bin -cp "lib/*" -sourcepath src src/aula/Main.java
java -cp "bin;lib/*" aula.Main
```

## Controles

- `W` `A` `S` `D`: anda (frente, esquerda, trás, direita).
- `Q` / `E`: gira a câmera.
- `Z` / `X`: zoom.
- `1` a `4`: projeção paralela, cavaleira, gabinete e perspectiva.
- Seta direita / esquerda: troca a animação do Ogro do meio (o nome aparece no título da janela).
- Segure `TAB` para wireframe.
- `ESC` fecha a janela.

## O que mudou em relação ao trabalho M2

- Antes: o Java calculava a pose interpolada a cada quadro e enviava para o VBO (`glBufferSubData`).
- Agora: todos os frames vão para o VBO uma única vez. O Java só escolhe `frameA`, `frameB` e `t`, e o vertex shader faz `mix(poseA, poseB, t)`.

## Onde está cada parte

- `shaders/vertexShader.txt`: interpolação `mix(poseA, poseB, t)` e projeção `projection * view * posicao`.
- `shaders/fragmentShader.txt`: cor da textura na coordenada `uv`.
- `aula/shaders/ShaderProgram.java`: classe do professor (lê, compila e liga os shaders).
- `aula/shaders/Md2Shader.java`: arquivos usados e atributos 0 `poseA`, 1 `poseB`, 2 `textuv`.
- `aula/md2/LeitorMd2.java`: leitura do MD2 (igual ao trabalho M2).
- `aula/md2/ModeloMd2.java`
  - `criaVBOs()`: coloca todos os frames, um depois do outro, no mesmo VBO.
  - `atualizar(dt)`: avança o tempo e calcula `frameA`, `frameB` e `t`.
  - `desenhase(shader)`: envia `t`, aponta o atributo 0 para o frame A e o atributo 1 para o frame B, e chama `glDrawArrays`.
- `aula/Main.java`: cria o shader, `enviaMatrizes()` (manda a projeção e a câmera para o shader) e o laço principal.

## Modelo

`md2/ogro.md2` e `md2/grok.jpg`: modelo Ogro, de Magarnigal (skin de Ogro_Fix), obtido dos exemplos do three.js. Créditos completos em `Ogro.txt`.
