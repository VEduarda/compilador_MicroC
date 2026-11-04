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

Outra melhoria fundamental implementada: a análise semântica permite shadowing,
e o compilador preserva estritamente os escopos e a identidade das variáveis na
representação intermediária e na memória do interpretador. Quando uma variável é
declarada em um escopo interno ou sombreia uma variável externa, o `TACTranslator`
gera um identificador único qualificado na IR (ex.: `x` no escopo 0 e `x_1`, `x_2`
nos blocos internos e irmãos). O `TACInterpreter` e a classe `ExecutionResult`
mantêm cada instância de variável separada em memória sem sobrescrita, permitindo
que variáveis de escopos distintos coexistam e sejam consultadas tanto pelo nome
base quanto por escopo (`getVariable("x")`, `getVariable("x", 1)`, etc.).

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

**Estado dos testes de otimização:** todas as verificações da bateria embutida
`OptimizationTestRunner` (39 checks no total) passam com **100% de sucesso (0 falhas)**:

- As identidades algébricas clássicas (`a + 0 -> a`, `0 + b -> b`, `c * 1 -> c`,
  `d * 0 -> 0`, `e - 0 -> e`, `f / 1 -> f`), bem como identidades booleanas e
  divisões, operam e simplificam corretamente as instruções TAC;
- A eliminação de subexpressões comuns (CSE) preserva os mapeamentos válidos no
  bloco básico e reutiliza variáveis existentes, invalidando apenas expressões
  cujos operandos ou destinos foram alterados;
- A eliminação de código morto (DCE) distingue programas com raízes observáveis
  de trechos isolados de temporárias, removendo temporárias não lidas quando
  apropriado e preservando resultados de testes;
- A preservação de escopos e identidade de variáveis em tempo de execução foi
  integrada à bateria e validada tanto para shadowing quanto para blocos irmãos.

## 9. Erros e testes disponíveis

O pipeline interrompe a compilação na primeira etapa que falha. Erros léxicos e
sintáticos incluem linha e descrição; erros semânticos incluem linha, regra e
mensagem.

Validação executada durante a atualização desta documentação:

| Comando/execução | Resultado |
|---|---|
| `& 'C:\Program Files\Java\jdk-21\bin\javac.exe' -encoding UTF-8 -d target/classes (Get-ChildItem -Recurse -Filter *.java src/main/java).FullName` | Compilação passou com código 0 |
| `java -cp target/classes main.Main` | Processo terminou com código 0 e executou todas as demonstrações |
| Bateria de otimização chamada pelo `Main` | 39 verificações; 0 falhas (100% aprovado) |
| Bateria SDT chamada pelo `Main` | 53 verificações passaram (100% aprovado) |
| Verificações focadas de `float` | `a + b` inferido como `float`; `3 + 2.5` executado como `5.5`; conversão `int` para `float` executada; `float` para `int` rejeitado na semântica |

A mensagem final do `Main` de “100% de sucesso” só é exibida quando todos os
testes e cenários são concluídos sem falhas. O pipeline de testes e os executáveis
do projeto (`Main`, `SDTTestRunner` e `OptimizationTestRunner`) propagam
estritamente o código de saída do processo:
- **Código `0`:** todos os testes e verificações passaram com sucesso (0 falhas);
- **Código diferente de zero (`1`):** pelo menos uma verificação falhou, ocorreu erro de
  execução ou divergência observável na preservação semântica.

Para compilar e executar a demonstração:

```powershell
javac -encoding UTF-8 -d target/classes (Get-ChildItem -Recurse -Filter *.java src/main/java).FullName
java -cp target\classes main.Main
```

## 10. Pendências para ampliar a cobertura

Para atender ao exemplo com tipos numéricos mistos e aproximar o projeto de um
compilador mais completo, o status das pendências é:

1. Implementar `const` no lexer, parser, AST, tabela de símbolos e impedir
   reatribuição.
2. Criar AST e tradução/execução para chamadas de função, com verificação de
   aridade e tipo dos argumentos e contexto de chamada/retorno.
3. Validar `return` conforme o tipo e a posição da função.
4. **[CONCLUÍDO]** Preservar escopos/identidade das variáveis na representação
   intermediária (`TACTranslator`) e na memória do interpretador (`TACInterpreter`
   e `ExecutionResult`).
5. **[CONCLUÍDO]** Implementar e corrigir as simplificações algébricas que falharam
   nos testes: `a + 0 -> a`, `0 + b -> b`, `c * 1 -> c`, `d * 0 -> 0`, `e - 0 -> e`
   e `f / 1 -> f`. Corrigir também a eliminação de subexpressões comuns (CSE).
   Garantir que todas as verificações passem com 100% de sucesso.
6. **[CONCLUÍDO]** Tornar o pipeline de testes capaz de reportar falha como resultado não-zero,
   para que verificações malsucedidas não sejam apresentadas como sucesso.
7. Criar testes automatizados de unidade e integração para cada etapa e para os
   exemplos exigidos.

Até essas pendências serem resolvidas e validadas, a formulação correta para a
entrega é que o projeto implementa um **subconjunto funcional de MicroC**, não
um compilador completo para todos os exemplos.
