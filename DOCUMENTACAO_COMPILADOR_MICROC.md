# Documentação do compilador MicroC

## 1. Objetivo e estado atual

O projeto implementa um compilador/interpretador para um subconjunto de MicroC,
com análise léxica, sintática e semântica, geração de código intermediário TAC,
otimização e execução por interpretador.

**Estado: funcional para exemplos limitados, mas ainda não é um compilador
100% completo.** Há recursos do enunciado e dos exemplos que não são suportados,
e parte dos testes de otimização falha. Esta documentação descreve o código que
existe, sem tratar funcionalidades futuras como implementadas.

> **Linguagem de implementação:** o código deste repositório está escrito em
> Java (`src/main/java`, `pom.xml`). MicroC é a linguagem-fonte analisada. Se o
> requisito da disciplina for implementar o compilador usando a linguagem C,
> esse requisito ainda não é atendido por esta versão.

### Tipos numéricos implementados

O lexer e o parser reconhecem agora este exemplo:

```c
int a = 3;
float b = 2.5;
```

O nó numérico da AST preserva se o literal é `int` ou `float`. Na análise
semântica, a expressão `a + b` tem tipo `float`, pois a aritmética promove o
resultado quando pelo menos um operando é `float`. Essa inferência também pode
ser consultada por `SemanticAnalyzer.getExpressionType(node)`.

Uma conversão implícita de `int` para `float` é permitida. Atribuição de `float`
para `int`, como `int x = 1.9;`, chega à análise semântica e é rejeitada para
evitar perda implícita de precisão. O back-end converte valores inteiros
atribuídos a variáveis `float` e executa aritmética decimal.

## 2. Arquitetura e fluxo de dados

```text
Fonte MicroC
    ↓
Lexer (tokens e erros léxicos)
    ↓
Parser (AST e erros sintáticos)
    ↓
SemanticAnalyzer (símbolos, escopos e erros semânticos)
    ↓
TACTranslator (código intermediário de três endereços)
    ↓
TACOptimizer (otimizações)
    ↓
TACInterpreter (execução e resultado)
```

O fluxo principal está em `backend/CompilerPipeline.java`. As etapas seguintes
só são executadas se a análise semântica terminar sem erros. O back-end escolhido
é um interpretador de TAC; não há geração de assembly, código objeto ou código
de máquina.

## 3. Organização do código

| Pacote | Responsabilidade |
|---|---|
| `lexer` | Tokens, leitura do código-fonte e erros léxicos |
| `parser` | Gramática, construção da AST e erros sintáticos |
| `ast` | Nós da AST, interface Visitor e serialização JSON |
| `semantic` | Tabela de símbolos, escopos, inferência e erros semânticos |
| `sdt` | Tradutores, impressão da AST e pool de temporárias |
| `optimizer` | Representação de instruções TAC e otimizações |
| `backend` | Pipeline de compilação, interpretador e resultado da execução |
| `main` | Demonstrações e chamadas das baterias de verificações |

## 4. Análise léxica

O lexer é implementado manualmente em `lexer/Lexer.java`, lendo o texto
caractere a caractere. Os tokens incluem:

- Palavras reservadas: `int`, `float`, `void`, `if`, `else`, `while`, `break`,
  `continue`, `return`, `print`.
- Identificadores, números inteiros, decimais no formato `dígitos.dígitos` e
  strings entre aspas duplas.
- Operadores aritméticos, relacionais, igualdade, lógicos e atribuição.
- Delimitadores `(){} , ;`.
- Comentários de linha (`//`) e de bloco (`/* ... */`).

Cada token guarda sua linha. Caracteres não reconhecidos e strings/comentários
sem fechamento geram `LexicalError` com a linha do erro.

**Limite:** notação científica, como `1e-3`, não é reconhecida; `const` também
não está implementado.

## 5. Análise sintática

O parser é descendente recursivo e constrói nós da AST. Para expressões, a
precedência implementada, da menor para a maior, é:

1. `||`
2. `&&`
3. `==`, `!=`
4. `<`, `>`, `<=`, `>=`
5. `+`, `-`
6. `*`, `/`
7. `!` e `-` unário
8. literais, identificadores e expressões entre parênteses

O subconjunto de comandos reconhecido inclui declarações, atribuições,
`print`, `if/else`, `while`, `break`, `continue`, `return` e definições de
função. Os parâmetros de função aceitos pela gramática são do tipo `int` ou
`float`.
Erros sintáticos são reportados com linha, token encontrado e contexto.

**Limites:** não há chamada de função como expressão/comando, nem suporte a
arrays, `for` ou outras construções que não estejam explicitamente listadas.
Embora `void` possa ser usado em definições, a análise semântica não valida
completude ou coerência dos retornos.

## 6. AST e análise semântica

`ast/Nodes.java` contém os nós do programa, declarações, expressões, controle
de fluxo, funções e retorno. Os nós preservam a linha de origem para mensagens
de erro.

`semantic/SymbolTable.java` usa uma pilha de mapas para resolução do escopo mais
interno para o mais externo e mantém histórico das declarações. O analisador
verifica:

- identificador não declarado;
- redeclaração no mesmo escopo;
- compatibilidade de tipos em inicialização e atribuição;
- compatibilidade de operandos;
- condição de `if`/`while` não string;
- uso antes de inicialização;
- `break` e `continue` fora de `while`;
- shadowing permitido durante a análise.

Os tipos efetivamente inferidos são `int`, `float` e `string` (para literais de
texto). A aritmética entre `int` e `float` promove o resultado para `float`;
comparações e operações lógicas retornam `int`. A atribuição permite conversão
implícita de `int` para `float`, mas rejeita conversão de `float` para `int`.
As regras e limitações dessa etapa também estão descritas em
[README_ANALISE_SEMANTICA.md](./README_ANALISE_SEMANTICA.md).

Funções são registradas na tabela, e parâmetros são inseridos no escopo da
função. Porém, não há chamada de função na gramática/AST, verificação de
argumentos nem validação do tipo/uso de `return`.

## 7. Código intermediário e execução

O `sdt/TACTranslator.java` percorre a AST com o padrão Visitor. O TAC inclui
instruções de cópia e operações, `print`, rótulos e saltos para controle de
fluxo, além de `return`. A tradução pode usar um `TempPool` para reutilizar
temporárias e pode fazer folding de constantes.

Exemplo de tradução:

```c
int x = 2 + 3;
print(x);
```

Forma intermediária ilustrativa:

```text
t1 = 2 + 3
x = t1
print x
```

O `backend/TACInterpreter.java` executa instruções TAC inteiras e decimais,
desvios, impressão, cópias, conversões de `int` para `float` e operadores
aritméticos/lógicos/relacionais. A execução é
limitada por um máximo de ciclos para interromper laços que não terminam. A
memória do interpretador é um mapa plano de nomes para valores.
Os valores `float` usam `double` do Java como representação numérica interna.

O suporte a funções é incompleto também no back-end: uma função gera um rótulo,
mas não há instrução de chamada nem pilha de chamadas. O interpretador pode
iniciar no rótulo `main`; uma instrução `return` encerra a execução atual do
programa, não retorna a um chamador.

Outra diferença importante: a análise semântica permite shadowing, mas o TAC
mantém o mesmo nome de variável e o interpretador usa memória plana. Assim, a
execução não distingue necessariamente variáveis de mesmo nome em escopos
diferentes.

## 8. Otimização

`optimizer/TACOptimizer.java` implementa passagens para:

- folding e propagação de constantes;
- propagação de cópias;
- simplificações algébricas;
- eliminação de subexpressões comuns (CSE);
- eliminação de código morto e instruções inalcançáveis;
- remoção de saltos redundantes e autoatribuições.

O enunciado pede que o comportamento antes e depois seja preservado. O exemplo
da seção de otimização do PDF (`t1 = 10 * 2`, `t2 = t1 + 5`, cópias até `x`)
é reduzido a `x = 25`, e a demonstração compara a execução do exemplo original
e otimizado.

**Estado dos testes de otimização:** na execução da bateria embutida
`OptimizationTestRunner`, 7 de 28 verificações falharam:

- seis identidades algébricas esperadas (`a + 0`, `0 + b`, `c * 1`, `d * 0`,
  `e - 0`, `f / 1`);
- uma verificação de eliminação de subexpressões comuns.

Logo, as técnicas estão presentes no código, mas não se deve afirmar que todas
funcionam ou que a otimização está totalmente validada.

## 9. Erros e testes disponíveis

O pipeline interrompe a compilação na primeira etapa que falha. Erros léxicos e
sintáticos incluem linha e descrição; erros semânticos incluem linha, regra e
mensagem.

Validação executada durante a atualização desta documentação:

| Comando/execução | Resultado |
|---|---|
| `mvn clean test` | Compilação passou; não há testes JUnit/TestNG em `src/test` |
| `java -cp target/classes main.Main` | Processo terminou com código 0 e executou as demonstrações |
| Bateria de otimização chamada pelo `Main` | 28 verificações; 7 falhas |
| Bateria SDT chamada pelo `Main` | 53 verificações passaram |
| Verificações focadas de `float` | `a + b` inferido como `float`; `3 + 2.5` executado como `5.5`; conversão `int` para `float` executada; `float` para `int` rejeitado na semântica |

O `Main` imprime uma mensagem final de “100% de sucesso” mesmo quando a bateria
de otimização reporta falhas. Portanto, para validar a otimização, deve-se
conferir os totais e as linhas `[FALHA]`, e não apenas essa mensagem final.
As baterias são executáveis Java embutidos, não testes descobertos pelo `mvn
test`. As verificações focadas de `float` foram executadas via JShell durante o
desenvolvimento e ainda não foram adicionadas como testes automatizados ao
projeto.

Para compilar e executar a demonstração:

```powershell
mvn test
java -cp target\classes main.Main
```

## 10. Pendências para ampliar a cobertura

Para atender ao exemplo com tipos numéricos mistos e aproximar o projeto de um
compilador mais completo, ainda é necessário:

1. Implementar `const` no lexer, parser, AST, tabela de símbolos e impedir
   reatribuição.
2. Criar AST e tradução/execução para chamadas de função, com verificação de
   aridade e tipo dos argumentos e contexto de chamada/retorno.
3. Validar `return` conforme o tipo e a posição da função.
4. Preservar escopos/identidade das variáveis na representação intermediária e
   na memória do interpretador.
5. Implementar e corrigir as simplificações algébricas que falharam nos testes:
   `a + 0 -> a`, `0 + b -> b`, `c * 1 -> c`, `d * 0 -> 0`, `e - 0 -> e` e
   `f / 1 -> f`. Corrigir também a eliminação de subexpressões comuns (CSE),
   cuja verificação falhou. Depois, executar novamente a bateria e garantir
   que todas as verificações passem.
6. Tornar o pipeline de testes capaz de reportar falha como resultado não-zero,
   para que verificações malsucedidas não sejam apresentadas como sucesso.
7. Criar testes automatizados de unidade e integração para cada etapa e para os
   exemplos exigidos.

Até essas pendências serem resolvidas e validadas, a formulação correta para a
entrega é que o projeto implementa um **subconjunto funcional de MicroC**, não
um compilador completo para todos os exemplos.
