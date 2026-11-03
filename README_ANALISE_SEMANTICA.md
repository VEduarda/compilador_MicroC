# Análise semântica do MicroC

Este arquivo documenta a etapa semântica existente no projeto. A visão de
arquitetura, o fluxo completo e o estado das funcionalidades estão em
[DOCUMENTACAO_COMPILADOR_MICROC.md](./DOCUMENTACAO_COMPILADOR_MICROC.md).

## Arquivos

```text
src/main/java/semantic/
├── Symbol.java
├── SymbolTable.java
├── SemanticError.java
└── SemanticAnalyzer.java
```

- `Symbol` guarda nome, tipo textual, categoria, escopo, linha, estado de
  inicialização e, quando conhecido, um valor constante.
- `SymbolTable` mantém uma pilha de mapas para os escopos ativos e um histórico
  dos símbolos declarados. A busca começa no escopo mais interno, permitindo
  shadowing durante a análise.
- `SemanticError` registra linha, identificador da regra e mensagem.
- `SemanticAnalyzer` percorre a AST e infere os tipos das expressões.

## Verificações implementadas

| Regra | Verificação |
|---|---|
| R1 | Uso de identificador não declarado |
| R2 | Redeclaração no mesmo escopo |
| R3 | Compatibilidade entre tipo declarado e inicializador/atribuição |
| R4 | Compatibilidade de operandos e operadores de expressões |
| R5 | Shadowing em escopo interno permitido |
| R6 | Condição de `if`/`while` não pode ser uma string |
| R7 | Uso de variável antes de inicialização |
| R8 | `break` e `continue` precisam estar dentro de `while` |

O analisador trata `int` e `float` como tipos numéricos, e `string` como tipo
derivado de literais de texto. Literais decimais usam o formato `dígitos.dígitos`.
Em operações aritméticas, misturar `int` e `float` promove o resultado para
`float`; comparações e operações lógicas retornam `int`. Atribuir `int` a `float`
é permitido por conversão implícita, mas atribuir `float` a `int` gera erro para
evitar perda silenciosa de precisão. Operações binárias com strings são rejeitadas.

Depois de analisar o programa, `SemanticAnalyzer.getExpressionType(node)` permite
consultar o tipo inferido de uma expressão, por exemplo, `float` para `a + b`
quando `a` é `int` e `b` é `float`. A consulta retorna `"erro"` se encontrar
inconsistência semântica.

## Funções: suporte parcial

A AST, o parser e o analisador semântico reconhecem definições de função com
parâmetros `int` ou `float` e registram a função e os parâmetros na tabela de símbolos.
Isso não equivale a suporte completo a funções: o parser não reconhece chamadas,
e a análise semântica não valida aridade, tipos dos argumentos ou compatibilidade
do `return` com o tipo de retorno declarado. Veja as limitações detalhadas na
[documentação geral](./DOCUMENTACAO_COMPILADOR_MICROC.md).

## Escopos e regras

O escopo global é criado pela tabela de símbolos. Blocos de `if` e `while`
abrem escopos; o corpo de função recebe um escopo para seus parâmetros e
variáveis. A tabela mantém o histórico para permitir a exibição das entradas
mesmo depois do fechamento do escopo.

O reconhecimento semântico de shadowing não garante que o back-end preserve
esse comportamento na execução: o TAC usa os nomes das variáveis diretamente,
e o interpretador mantém uma única memória plana. Essa diferença está registrada
como limitação do back-end na documentação geral.
