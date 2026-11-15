# Uso do SDK Java

Os fragmentos deste guia usam uma instância aberta de `Cliente`. Em aplicações que atendem várias requisições, mantenha essa instância durante o ciclo de vida da aplicação e feche-a no encerramento. O cliente é síncrono e pode ser compartilhado entre threads.

## Configuração e credenciais

`new Cliente()` resolve `MAILCORE_API_KEY` a cada operação. `new Cliente(chave)` usa uma chave explícita. Para alterar a configuração:

```java
Configuracao configuracao = Configuracao.construtor()
        .tempoLimite(Duration.ofSeconds(45))
        .tempoConexao(Duration.ofSeconds(10))
        .limiteRespostaBytes(32L * 1024 * 1024)
        .construir();

Cliente cliente = new Cliente(null, configuracao);
```

Os tipos públicos são de `br.com.mailcore.sdk`; `Duration` é de `java.time`. A origem padrão é `https://mailcore.com.br/v1`. `urlBase(...)` conserva o prefixo da API ao acrescentar as rotas dos recursos. HTTP somente é aceito com `permitirHttpLocal(true)` e host de loopback, para ensaios locais.

`fornecedorChave(Supplier<String>)` permite consultar uma fonte de credenciais a cada chamada. Use `new Cliente(null, configuracao)` nesse caso; chave explícita e fornecedor juntos são recusados. O fornecedor deve ser rápido e seguro para concorrência. Exceções de aquisição não expõem a causa original.

`certificadoCa(byte[] pem)` acrescenta certificados de uma autoridade privada às autoridades padrão, mantendo a verificação de hostname. Não há opção de desativar TLS. Com `clienteHttp(OkHttpClient)`, a aplicação assume as políticas do cliente injetado, incluindo TLS, proxies, cookies, interceptores e retentativas. Não combine uma CA configurada no SDK com um cliente HTTP injetado.

## Opções por operação

```java
Map<String, List<String>> cabecalhos = new LinkedHashMap<String, List<String>>();
cabecalhos.put("X-Correlacao", Collections.singletonList("pedido-123"));

OpcoesRequisicao opcoes = OpcoesRequisicao.construtor()
        .chaveIdempotencia("pedido-123-confirmacao")
        .tempoLimite(Duration.ofSeconds(60))
        .limiteRespostaBytes(64L * 1024 * 1024)
        .cabecalhos(cabecalhos)
        .construir();

Resposta resposta = cliente.emails().enviar(pedido, opcoes);
```

`null` em `opcoes` e a sobrecarga sem opções usam os padrões do cliente. As opções são imutáveis; mapas e listas de cabeçalhos são copiados. Cabeçalhos HTTP reservados, como `Authorization`, `Host`, `Cookie`, `Content-Type`, `Accept` e `Idempotency-Key`, não podem ser substituídos por `cabecalhos(...)`. Configure a idempotência no campo próprio.

`PedidoEmail.cabecalhos(...)` pertence à mensagem de e-mail; `OpcoesRequisicao.cabecalhos(...)` pertence à requisição HTTP. A API valida os cabeçalhos de mensagem permitidos.

## Prazos, cancelamento e encerramento

`tempoLimite` é o prazo da chamada HTTP, incluindo conexão, handshake TLS e leitura da resposta. `tempoConexao` limita o estabelecimento da conexão TCP, sem substituir o prazo total. Os padrões são 30 e 10 segundos. Prazos devem ser positivos e não ultrapassar 24 horas.

A serialização do pedido ocorre antes do transporte. Fornecedores de chave e consumidores em fluxo executam código da aplicação e não são interrompidos à força. O SDK confere o tempo decorrido ao recuperar o controle; esses callbacks precisam cooperar para que o prazo seja observado prontamente.

```java
SinalCancelamento sinal = new SinalCancelamento();
OpcoesRequisicao opcoes = OpcoesRequisicao.construtor()
        .sinalCancelamento(sinal)
        .construir();
```

Passe essas opções à operação. Outra thread pode chamar `sinal.cancelar()` para cancelar as chamadas inscritas. O sinal pode agrupar operações e permanece cancelado; crie outro sinal para trabalho novo. `cliente.fechar()` cancela chamadas em curso, impede chamadas futuras e pode ser repetido. Um cliente HTTP injetado continua pertencendo à aplicação; seu pool não é encerrado pelo SDK.

## Conteúdo e anexos

`PedidoEmail` representa envio direto ou por modelo. Os builders de `PedidoEmail`, `AnexoEmail` e `ModeloEmail` produzem snapshots JSON; alterações posteriores nas coleções fornecidas não modificam o objeto construído.

Envio direto aceita HTML/texto, cópias, cópias ocultas, resposta, cabeçalhos e anexos conforme o contrato HTTP. Destinatários e cópias usam endereços sem nome de exibição; o remetente pode incluir um nome. Por exemplo:

```java
AnexoEmail anexo = AnexoEmail.construtor()
        .nome("marca.png")
        .tipoConteudo("image/png")
        .conteudoBase64(conteudoBase64)
        .idConteudo("marca")
        .construir();

PedidoEmail pedido = PedidoEmail.construtor()
        .remetente("Equipe <notificacoes@seudominio.com.br>")
        .destinatarios(Collections.singletonList("cliente@example.com"))
        .assunto("Pedido recebido")
        .html("<img src=\"cid:marca\" alt=\"Marca\"><p>Recebemos seu pedido.</p>")
        .anexos(Collections.singletonList(anexo))
        .construir();
```

`conteudoBase64` é uma String Base64 padrão, sem prefixo `data:`. O identificador CID deve corresponder ao usado no HTML. O SDK envia o conteúdo fornecido e não baixa anexos remotos.

## Modelos e lotes

```java
Map<String, Object> variaveis = new LinkedHashMap<String, Object>();
variaveis.put("NOME", "Ana");

ModeloEmail modelo = ModeloEmail.construtor()
        .id("boas-vindas")
        .variaveis(variaveis)
        .construir();

PedidoEmail pedido = PedidoEmail.construtor()
        .destinatarios(Collections.singletonList("cliente@example.com"))
        .modelo(modelo)
        .construir();
```

Use um modelo publicado. Essa modalidade aceita anexos Base64 comuns, sem `idConteudo`/CID, pelo mesmo campo `anexos(...)` do builder. HTML/texto, Cc/Cco, cabeçalhos, nome de exibição no remetente e anexos CID não podem ser combinados com modelo. Os padrões publicados podem fornecer remetente, assunto e resposta. A API valida as combinações, estados e permissões.

`cliente.lotes().enviar(lista, opcoes)` envia um array de 1 a 100 mensagens numa única requisição, sem dividir o lote. O contrato de lote não aceita Cc/Cco, cabeçalhos, nome de exibição no remetente ou anexos. O servidor aplica a aceitação atômica e conserva a ordem. Um objeto no lugar do array raiz é recusado antes da rede.

Agendamento usa `agendada_em` no DTO HTTP; no builder, use `agendadaEm(...)` com uma data ISO 8601 que inclua o fuso. Reagendamento e cancelamento dependem do estado da intenção no servidor.

## Mapas, POJOs e JSON

Recursos aceitam mapas e objetos que Jackson consiga serializar. O JSON final deve ser um objeto, exceto no envio de lotes, que exige array. Campos mantêm o nome do DTO da API, como `remetente`, `destinatarios`, `revisao` e `agendada_em`. `null` e `false` dentro do objeto são preservados; `null` na raiz não substitui um objeto.

Os métodos com corpo opcional oferecem uma conveniência que envia `{}`, por exemplo `cliente.modelos().duplicar(id)`. Consultas opcionais oferecem chamadas como `cliente.emails().listar()`. Para adicionar opções numa listagem filtrada, use `listar(filtros, opcoes)`; para opções sem filtros, passe um mapa vazio no primeiro argumento.

Valores numéricos devem ser finitos e textos devem conter Unicode válido. O teto geral do corpo JSON é 50 MiB. Esse teto acomoda escapes das importações CSV, cujo limite de negócio é 8 MiB; ele não amplia os limites menores dos outros endpoints. Não há validação completa de todos os schemas de negócio no SDK.

## Respostas, erros e paginação

```java
Resposta resposta = cliente.emails().obter(id);
JsonNode dados = resposta.json();
String estado = dados.path("estado").asText();
```

`JsonNode` pertence a `com.fasterxml.jackson.databind`. Use `resposta.decodificar(SeuTipo.class)` ou `decodificar(TypeReference<T>)` para tipos próprios. Os bytes originais não passam por conversão numérica intermediária. O parser JSON usa inteiros e decimais de precisão arbitrária; o tipo de destino escolhido na decodificação determina suas conversões.

`dados()` retorna cópia do array e `cabecalhos()` é um mapa imutável. Downloads preservam os bytes, inclusive MIME e conteúdo não UTF-8. Respostas sem conteúdo retornam um array vazio, mantendo status e cabeçalhos; a exclusão pode concluir com HTTP 204.

```java
try {
    cliente.emails().obter(id);
} catch (ErroMailcore erro) {
    String codigo = erro.codigo();
    Integer status = erro.status();
    // Classifique a falha sem registrar o corpo ou a mensagem privada da API.
}
```

`ErroMailcore` é não verificada e separa código, mensagem de negócio, status opcional e cabeçalhos permitidos. Falhas de rede não têm necessariamente um status. Entre os códigos locais estão `configuracao_invalida`, `falha_de_rede`, `prazo_excedido`, `cancelado`, `cliente_fechado`, `resposta_invalida`, `resposta_incompleta`, `resposta_excedida`, `codificacao_inesperada`, `redirecionamento_recusado` e `erro_consumidor`.

Cada listagem consulta uma página. O SDK não percorre cursores automaticamente nem altera o envelope recebido. Use `proximo` ou `proximo_cursor` como `antes`, conforme o recurso. Recebimento também aceita `depois`, sem combiná-lo com `antes`. Na query, valores `null` são omitidos e `false` é enviado como `false`.

## Downloads em fluxo

`contatos().exportacoes().baixarFluxo(...)`, `recebimento().baixarOriginalFluxo(...)` e `recebimento().baixarAnexoFluxo(...)` recebem `ConsumidorFluxo` e fazem a leitura síncrona de blocos. Cada bloco pertence ao consumidor e pode ser retido. A leitura do próximo bloco aguarda o retorno de `receber(...)`.

```java
OpcoesRequisicao opcoes = OpcoesRequisicao.construtor()
        .limiteRespostaBytes(64L * 1024 * 1024)
        .tempoLimite(Duration.ofSeconds(60))
        .construir();

try (OutputStream saida = Files.newOutputStream(destino,
        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
    Resposta download = cliente.recebimento().baixarOriginalFluxo(id, saida::write, opcoes);
    long totalGravado = download.totalBytes();
}
```

Nesse fragmento, `destino` é um `Path` escolhido pela aplicação; `OutputStream` pertence a `java.io` e os demais tipos de arquivo a `java.nio.file`. Sucesso retorna `dados()` vazio e `totalBytes()` com a quantidade entregue. Uma falha no consumidor encerra a leitura com erro; a aplicação trata o arquivo parcial. O limite de bytes é aplicado durante a leitura, inclusive em fluxo e em respostas HTTP de erro.

As variantes sem `Fluxo` acumulam a resposta em memória. As rotas de download são autenticadas e fixas no Mailcore. URLs temporárias contidas em metadados não são seguidas automaticamente e não recebem a chave da API.

## Idempotência e retentativas

Persista a chave e o conteúdo da intenção antes de operações que suportam idempotência. Em caso de repetição decidida pela aplicação, reutilize ambos. `emails().obterPorIdempotencia(chave)` e `lotes().obterPorIdempotencia(chave)` permitem conciliação com permissão de leitura; a retenção da chave é determinada pelo contrato do servidor.

O SDK não implementa fila, backoff ou retentativas de negócio. O transporte padrão desativa a recuperação automática de falhas de conexão. Mutações usam um corpo de uso único, inclusive quando vazio, impedindo sua reemissão pelo OkHttp. GET pode ter follow-up interno em HTTP 503 com `Retry-After: 0` ou HTTP 421 em conexões HTTP/2 coalescidas. Assim, uma operação de leitura não promete exatamente uma tentativa de rede. Um cliente HTTP injetado possui suas próprias políticas.

Não deduza rejeição de uma intenção a partir de timeout, cancelamento ou queda de conexão. A aceitação pode ter ocorrido antes da interrupção da resposta. Conflitos de revisão HTTP 409 continuam sob decisão da aplicação; o SDK não atualiza a revisão nem sobrescreve uma edição concorrente.

## Verificar webhooks

```java
EventoWebhook evento = Assinatura.verificar(corpoBruto, cabecalhosRecebidos, segredoWebhook);
String tipo = evento.tipo();
JsonNode dados = evento.dados();
```

`corpoBruto` é o `byte[]` original, antes de qualquer parser ou recodificação; os cabeçalhos usam `Map<String, List<String>>`. O verificador usa HMAC-SHA256, segredo `whsec_...` e `svix-id`, `svix-timestamp`, `svix-signature`. A tolerância padrão e máxima é de cinco minutos; `OpcoesAssinatura` permite reduzi-la e injetar o instante em testes.

Processe o evento somente após a verificação. Faça a deduplicação de `svix-id` em armazenamento durável da aplicação; o verificador não mantém estado. Corpo, dados do evento e mensagem de erro podem conter informações privadas.
