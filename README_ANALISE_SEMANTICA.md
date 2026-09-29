# Analisador Semântico do MicroC — o que foi adicionado

Este documento explica **apenas a parte nova** (análise semântica), construída em
cima do analisador léxico e sintático que vocês já tinham prontos. Nenhum arquivo
de `lexer` ou `parser` foi reescrito — só recebeu pequenos ajustes pontuais
(explicados na seção 3).

## 1. Arquivos novos (pacote `semantic`)

```
src/main/java/semantic/
├── Symbol.java            -> uma linha da Tabela de Símbolos
├── SymbolTable.java        -> a Tabela de Símbolos em si (pilha de escopos)
├── SemanticError.java      -> um erro semântico (linha + regra + mensagem)
└── SemanticAnalyzer.java   -> percorre a AST e aplica as regras semânticas
```

### Symbol
Guarda `name`, `type`, `category` (VARIABLE/PARAMETER/FUNCTION — os dois
últimos já deixados prontos para quando o grupo quiser estender a linguagem
com funções), `scopeLevel`, `line` e se já foi `initialized`.

### SymbolTable
Implementada como **pilha de escopos** (`Deque<Map<String,Symbol>>`).
- `enterScope()` / `exitScope()` — abrem/fecham um escopo a cada `{ }`.
- `declare(symbol)` — insere no escopo atual; retorna `false` se já existir
  o mesmo nome **nesse mesmo escopo** (redeclaração).
- `lookup(name)` — procura do escopo mais interno para o mais externo (é
  isso que faz o sombreamento/shadowing funcionar automaticamente).
- Mantém também um **histórico** de todos os símbolos já declarados, para
  imprimir a Tabela de Símbolos completa ao final, mesmo depois que os
  escopos internos já foram fechados.

### SemanticAnalyzer
Percorre a AST (mesmo padrão de `Node.print()`/`AstJson`, usando
`instanceof`) e aplica as regras semânticas descritas na próxima seção.

## 2. Regras semânticas implementadas

| Regra | Descrição | Exemplo que dispara o erro |
|---|---|---|
| R1 | Variável usada sem ter sido declarada | `z = 5;` sem `int z;` antes |
| R2 | Redeclaração no **mesmo** escopo | `int x; int x;` no mesmo bloco |
| R3 | Tipo incompatível em declaração/atribuição | `int y = "texto";` |
| R4 | Operandos incompatíveis em expressão binária | `"abc" + 1` |
| R5 | Sombreamento (shadowing) em escopo mais interno | **permitido**, não gera erro |
| R6 | Condição de `if`/`while` não pode ser `string` | `if ("erro") { }` |
| R7 (extra) | Uso de variável antes de receber valor | `int x; print(x);` |

Essas regras cobrem diretamente os Exercícios **1, 2, 3, 4, 6, 7 e 9** do PDF.

## 3. Pequenos ajustes no código existente (não muda a linguagem)

O único problema é que os nós da AST não guardavam a **linha** do código-fonte,
e o PDF pede que todo erro semântico aponte a linha. Então:

- `ast/Node.java`: ganhou um campo `public int line` e um método fluente
  `.at(line)`.
- `parser/Parser.java`: cada `new XyzNode(...)` passou a terminar com
  `.at(token.line)`. Nenhuma regra gramatical mudou — é só "carimbar" a
  linha no nó que já era criado.
- `main/Main.java`: depois de montar a AST, agora chama o
  `SemanticAnalyzer`, imprime a Tabela de Símbolos e a lista de erros
  semânticos, e força a saída do console em UTF-8 (para os acentos não
  quebrarem em qualquer terminal).

## 4. ⚠️ Limitação importante: funções, `float` e `const`

O PDF de exercícios (Ex. 1, 3, 5, 8, 10) usa `float`, `const` e **funções com
parâmetros e retorno**. A gramática atual do MicroC (`lexer`/`parser`) ainda
**não reconhece** esses recursos — só existe `int`, `if/else`, `while`,
`print`, declaração e atribuição.

Por isso o `SemanticAnalyzer` foi construído para o que a linguagem já
suporta hoje, e cobre integralmente os exercícios 2, 4, 6, 7 e 9. Os
exercícios que envolvem função/float/const podem ser resolvidos **no papel**
usando a mesma lógica implementada aqui (a Tabela de Símbolos e as regras
R1–R4 são exatamente as mesmas que se aplicariam a funções e parâmetros).

Se o grupo quiser que o analisador rode esses exercícios de verdade, o
próximo passo é estender `TokenType`/`Lexer`/`Parser`/`Nodes` com:
- token e palavra reservada `float`, `const`, `void`, `return`, `,`
- `FunctionNode` (nome, tipo de retorno, lista de parâmetros, corpo)
- `CallNode` (nome da função, lista de argumentos)
- `ReturnNode`

e depois adicionar em `SemanticAnalyzer`:
- checagem de aridade e tipo dos argumentos numa chamada (Exercício 5)
- checagem de que o `return` é compatível com o tipo de retorno da função
- registro de `const` como símbolo "não reatribuível" (erro se houver
  atribuição posterior)

Posso te ajudar a fazer essa extensão também — é só pedir.

## 5. Como rodar

```bash
mvn compile exec:java -Dexec.mainClass=main.Main
```

ou, sem Maven:

```bash
javac -d out $(find src/main/java -name "*.java")
java -cp out main.Main
```

A saída mostra, nesta ordem: tokens, AST (texto e JSON), **Tabela de
Símbolos**, **lista de erros semânticos** e estatísticas de tempo de cada
fase (léxica, sintática e agora também semântica).

O `Main.java` já vem com um programa de exemplo que dispara de propósito
três erros (redeclaração, tipo incompatível e variável não declarada) e
mostra um caso de sombreamento válido, para vocês apresentarem o
funcionamento na entrega do trabalho.
