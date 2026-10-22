# Trabalho M2: animação MD2 (Quake 2)

Baseado no código `lwjgl-vbo-obj-maven_v5`, organizado como o trabalho CG_4_1 (leitor com `carregar(...)`, `criaCenario`, `simulaMundo`, câmera com WASD). Carrega um modelo MD2, interpola os frames da animação, aplica a textura e executa as animações.

Requisitos: JDK 17+ e driver com suporte a OpenGL 2.1. As bibliotecas do LWJGL (Windows) estão em `lib/`.

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

## Onde está cada parte

- `aula/md2/LeitorMd2.java`
  - `carregar(arquivo, tamanho, x, y, z, rotY)`: lê o cabeçalho, as coordenadas de textura, os triângulos e os frames do MD2.
  - `normaliza(...)`: mesma ideia do LeitorObj (tamanho, centro, base no chão, rotY).
- `aula/md2/ModeloMd2.java`
  - `separaAnimacoes()`: agrupa os frames pelo nome (stand01, stand02... vira "stand").
  - `atualizar(dt)`: avança o tempo e interpola os vértices entre dois frames, `v = a + (b - a) * t`, atualizando o VBO.
  - `desenhase()`: desenha com a textura usando os VBOs de posição e UV.
- `aula/Main.java`: `criaCenario()`, `simulaMundo(dt)`, `atualizaCamera()`, `desenhaChao()` e o laço principal.

## Modelo

`md2/ogro.md2` e `md2/grok.jpg`: modelo Ogro, de Magarnigal (skin de Ogro_Fix), obtido dos exemplos do three.js. Créditos completos em `Ogro.txt`.
