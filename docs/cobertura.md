# Cobertura do SDK Java

A versão `0.1.0-alpha.1` oferece **16 recursos principais, 30 classes de recursos e 151 operações HTTP síncronas**. Sobrecargas com opções e conveniências de argumentos opcionais não representam operações adicionais. As coordenadas `br.com.mailcore:mailcore-java` ainda não foram publicadas no Maven Central.

As assinaturas usam nomes Java em camelCase; o corpo mantém os campos em português do contrato HTTP. Identificadores são codificados como um único segmento de URI, sem perder o prefixo da API.

## Operações

Os caminhos abaixo partem de uma instância de `Cliente`. Todo método HTTP retorna `Resposta` ou lança `ErroMailcore` e possui uma sobrecarga com `OpcoesRequisicao` no último argumento.

| Recurso | Métodos | Operações |
| --- | --- | ---: |
| `emails()` | `enviar`, `listar`, `obter`, `reagendar`, `cancelar`, `obterPorIdempotencia`, `eventos`, `baixarAnexo` | 8 |
| `lotes()` | `enviar`, `obterPorIdempotencia` | 2 |
| `dominios()` | `criar`, `listar`, `obter`, `atualizar`, `remover`, `registrosDns`, `verificar` | 7 |
| `chavesApi()` | `criar`, `listar`, `renomear`, `revogar` | 4 |
| `contatos().segmentos()` | `listar`, `adicionar`, `remover` | 3 |
| `contatos().topicos()` | `listar`, `atualizar` | 2 |
| `contatos().importacoes()` | `prever`, `criar`, `obter` | 3 |
| `contatos().exportacoes()` | `criar`, `listar`, `obter`, `baixar`, `baixarFluxo` | 5 |
| `contatos()` | `criar`, `listar`, `obter`, `atualizar`, `remover`, `adicionarEmLote`, `alterarEmLote` | 7 |
| `segmentos()` | `criar`, `listar`, `obter`, `atualizar`, `remover` | 5 |
| `topicos()` | `criar`, `listar`, `obter`, `atualizar`, `remover` | 5 |
| `propriedadesContatos()` | `criar`, `listar`, `obter`, `atualizar`, `remover` | 5 |
| `modelos().versoes()` | `listar`, `obter`, `restaurar`, `prever` | 4 |
| `modelos().pastas()` | `listar`, `criar`, `atualizar`, `remover` | 4 |
| `modelos()` | `criar`, `listar`, `obter`, `atualizar`, `remover`, `prever`, `obterPrevia`, `testar`, `publicar`, `duplicar` | 10 |
| `webhooks().eventos()` | `listar`, `obter`, `listarTentativas`, `reenviar` | 4 |
| `webhooks()` | `criar`, `listar`, `obter`, `atualizar`, `remover` | 5 |
| `campanhas()` | `criar`, `listar`, `obter`, `atualizar`, `remover`, `duplicar`, `revisar`, `enviar`, `cancelar`, `prever`, `testar`, `lerPrevia`, `obterMetricas`, `listarDestinatarios`, `listarLinksClicados` | 15 |
| `automacoes().execucoes()` | `listar`, `obter` | 2 |
| `automacoes()` | `criar`, `listar`, `obter`, `atualizar`, `remover`, `duplicar`, `parar` | 7 |
| `eventos()` | `criar`, `listar`, `obter`, `atualizar`, `remover`, `enviar` | 6 |
| `recebimento()` | `listar`, `obter`, `listarAnexos`, `obterAnexo`, `baixarOriginal`, `baixarAnexo`, `baixarOriginalFluxo`, `baixarAnexoFluxo` | 8 |
| `caixas().conversas().emails()` | `listar`, `obter`, `responder`, `encaminhar` | 4 |
| `caixas().conversas().etiquetas()` | `adicionar`, `substituir`, `remover` | 3 |
| `caixas().conversas()` | `listar`, `buscar`, `obter`, `atualizar`, `remover` | 5 |
| `caixas().etiquetas()` | `listar`, `obter`, `criar`, `atualizar`, `remover` | 5 |
| `caixas().rascunhos()` | `listar`, `obter`, `criar`, `atualizar`, `remover`, `enviar` | 6 |
| `caixas()` | `criar`, `listar`, `obter`, `atualizar`, `remover` | 5 |
| `cli().concessoes()` | `listar`, `revogar` | 2 |
| `cli()` | Agrega `concessoes()`. | 0 |
| **Total** | **151 operações** | **151** |

Os recursos filhos compartilham o transporte e o pool do cliente. Os getters não fazem requisições. `cli()` é um agregador; as duas operações pertencem a `cli().concessoes()`.

## Pedidos e respostas

- `PedidoEmail`, `AnexoEmail` e `ModeloEmail` oferecem builders que preservam um snapshot JSON. `RespostaEnvio` representa a aceitação de uma intenção. Recursos complexos também aceitam mapas e POJOs serializáveis, mantendo os DTOs HTTP.
- O JSON final exige objeto na raiz; lotes exigem array. Raiz `null`, forma incompatível, texto Unicode inválido e números não finitos são recusados. `null` e `false` dentro de campos são preservados. Na query, `null` é omitido e `false` permanece `false`.
- `Resposta` conserva bytes originais, status, cabeçalhos e quantidade lida. `dados()` fornece uma cópia; `json()` e `decodificar(...)` interpretam os bytes sob decisão do chamador. Respostas sem conteúdo conservam array vazio.
- A paginação é explícita: uma operação consulta uma página e conserva o envelope e os cursores do recurso. Não existe varredura automática de todas as páginas.

## Contratos de negócio

O servidor valida permissões, escopos, formatos, revisões e estados. Envio direto aceita composição e anexos Base64/CID. Envio individual por modelo aceita anexos Base64 sem CID e não combina com HTML/texto, Cc/Cco, cabeçalhos ou nome de exibição no remetente. Lotes contêm de 1 a 100 mensagens e não aceitam Cc/Cco, cabeçalhos, nome de exibição no remetente ou anexos. A aceitação de uma intenção não comprova entrega SMTP.

Atualizações e ações em modelos, campanhas, automações, caixas e rascunhos conservam os campos de revisão exigidos pela API. Um conflito 409 é devolvido à aplicação. A biblioteca não inventa revisões nem refaz a edição.

Importações recebem CSV UTF-8 no pedido JSON. O limite de negócio do CSV é 8 MiB; o limite geral de serialização JSON é 50 MiB para acomodar escapes. Limites específicos menores permanecem sob validação da API.

A chave secreta de API só é retornada na criação. Concessões CLI permitem consulta e revogação; não iniciam autenticação interativa ou renovação de credenciais. O SDK não utiliza rotas privadas de sessão ou dashboard.

## Conteúdo recebido e fluxo

`recebimento()` consulta mensagens, anexos e original MIME pelas rotas autenticadas. `baixarOriginalFluxo`, `baixarAnexoFluxo` e `contatos().exportacoes().baixarFluxo` entregam blocos ao `ConsumidorFluxo`, com espera pelo retorno do consumidor antes da próxima leitura. Os blocos podem ser retidos pelo chamador. Sucesso em fluxo retorna `dados()` vazio e `totalBytes()` com o total entregue.

Downloads comuns acumulam bytes em memória. O limite padrão de resposta é 16 MiB, aplicado durante a leitura, inclusive em fluxo e em erros HTTP. Ajuste-o por operação conforme o orçamento da aplicação. Uma resposta detalhada com HTML `data_uri` pode crescer devido à expansão dos anexos; consulte com `formato_html: cid` ou estabeleça um limite apropriado.

URLs temporárias em metadados são capacidades sensíveis. A biblioteca não as segue automaticamente nem envia Bearer para destinos extraídos dessas respostas. Retenção e disponibilidade dos conteúdos são determinadas pelo servidor.

## Transporte e alcance

O cliente padrão reutiliza OkHttp, exige HTTPS verificado, não segue redirecionamentos, não mantém cookies e ignora proxies do ambiente. HTTP de teste depende de habilitação explícita e se limita a loopback. CA privada é configurável; o cliente HTTP injetado pertence à aplicação e mantém suas políticas.

O SDK solicita codificação `identity` e recusa codificações de conteúdo não negociadas, preservando bytes de CSV, MIME e anexos. O prazo padrão da chamada é 30 segundos; a conexão TCP possui limite de 10 segundos. A execução síncrona não interrompe à força callbacks fornecidos pela aplicação.

Mutações usam corpo de uso único e não são reemitidas pelo motor HTTP padrão. GET pode ter follow-up do OkHttp em 503 com `Retry-After: 0` ou 421 em conexão HTTP/2 coalescida. O SDK não mantém filas, backoff ou retentativas de negócio. Consulte [uso.md](uso.md) para conciliação por idempotência e cancelamento.

A verificação de webhook é local por `Assinatura.verificar(...)`, com HMAC-SHA256 e tolerância máxima de cinco minutos. O processamento e a deduplicação durável continuam na aplicação.

## Verificação

Os 151 contratos possuem casos parametrizados independentes da especificação do gerador. Cada caso chama o cliente público sobre HTTP local real e verifica método, caminho codificado, query, corpo, opções, resposta vazia e variantes de download aplicáveis. Esses testes usam dados sintéticos e não constituem implantação, entrega SMTP ou medição de desempenho em produção.

As fontes dos recursos são estáticas. `python3 scripts/gerar_recursos.py --verificar` confere sua correspondência com a especificação local, sem depender de outro projeto. O gerador não participa da execução do SDK.
