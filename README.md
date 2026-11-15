# SDK Mailcore para Java

[Código-fonte](https://github.com/mailcore-hq/mailcore-java) · [Problemas e sugestões](https://github.com/mailcore-hq/mailcore-java/issues)

Cliente síncrono da API Mailcore para **Java 8 ou superior**, com 151 operações, recursos modulares, respostas em bytes e JSON, downloads em fluxo e verificação de webhooks. Compartilhe uma instância de `Cliente` entre as threads da aplicação para reutilizar o pool HTTP.

Aplicações consumidoras usam Java e as dependências Maven. Docker serve como ambiente de desenvolvimento e verificação desta biblioteca.

Versão **`0.1.0-alpha.1`**, coordenadas **`br.com.mailcore:mailcore-java`**. Esta versão ainda não foi publicada no Maven Central. Consulte o [guia de uso](docs/uso.md) e a [cobertura da API](docs/cobertura.md).

## Instalação local

No ambiente de desenvolvimento da biblioteca, com Maven, JDK 17, Python 3 e OpenSSL para os testes:

```bash
mvn install
```

O build produz bytecode Java 8 e instala o artefato no repositório Maven local. No projeto consumidor:

```xml
<dependency>
  <groupId>br.com.mailcore</groupId>
  <artifactId>mailcore-java</artifactId>
  <version>0.1.0-alpha.1</version>
</dependency>
```

O JAR principal não inclui suas dependências; Maven resolve o transporte e o suporte JSON. `target/` também recebe os JARs de fontes e Javadoc. Copiar apenas o JAR principal para um classpath sem suas dependências não equivale à instalação Maven.

Para construir e executar os testes com as ferramentas dentro do Docker:

```bash
docker build -t mailcore/java-testes:17 .
docker run --rm --network none mailcore/java-testes:17
```

Nenhum serviço ou porta precisa ser publicado para os testes sintéticos locais.

Para testar todas as versões suportadas sem instalar JDK ou Maven no host, execute `python3 scripts/verificar.py`. A [verificação](docs/verificacao.md) também mostra como preparar os JARs e provar sua instalação num consumidor Maven separado e offline. Um exemplo compilável está em [exemplos/envio/EnviarEmail.java](exemplos/envio/EnviarEmail.java).

## Enviar o primeiro e-mail

Configure `MAILCORE_API_KEY` no ambiente protegido da aplicação. Use um domínio verificado e uma chave com permissão de envio.

```java
import br.com.mailcore.sdk.Cliente;
import br.com.mailcore.sdk.OpcoesRequisicao;
import br.com.mailcore.sdk.PedidoEmail;
import br.com.mailcore.sdk.Resposta;
import br.com.mailcore.sdk.RespostaEnvio;
import java.util.Collections;

public final class EnviarEmail {
    public static void main(String[] argumentos) {
        PedidoEmail pedido = PedidoEmail.construtor()
                .remetente("notificacoes@seudominio.com.br")
                .destinatarios(Collections.singletonList("cliente@example.com"))
                .assunto("Pedido recebido")
                .html("<p>Recebemos seu pedido.</p>")
                .texto("Recebemos seu pedido.")
                .construir();

        OpcoesRequisicao opcoes = OpcoesRequisicao.construtor()
                .chaveIdempotencia("pedido-123-confirmacao")
                .construir();

        try (Cliente cliente = new Cliente()) {
            Resposta resposta = cliente.emails().enviar(pedido, opcoes);
            RespostaEnvio envio = resposta.decodificar(RespostaEnvio.class);
            System.out.println(envio.id());
        }
    }
}
```

Uma aceitação HTTP 202 registra a intenção de envio. Consulte os eventos ou processe webhooks para acompanhar a entrega. Na aplicação de longa duração, reutilize o cliente e feche-o somente ao encerrar seu ciclo de vida. `close()` e `fechar()` são equivalentes.

Os métodos também aceitam mapas e objetos serializáveis como JSON. O exemplo abaixo envia os campos do mesmo contrato HTTP:

```java
Map<String, Object> pedido = new LinkedHashMap<String, Object>();
pedido.put("remetente", "notificacoes@seudominio.com.br");
pedido.put("destinatarios", Collections.singletonList("cliente@example.com"));
pedido.put("assunto", "Pedido recebido");
pedido.put("html", "<p>Recebemos seu pedido.</p>");
Resposta resposta = cliente.emails().enviar(pedido);
```

Nesse fragmento, `cliente` é uma instância aberta e os tipos de coleção pertencem a `java.util`. Campos `null` e `false` são preservados no corpo. Um `null` no lugar do objeto raiz é recusado.

## Respostas e erros

`Resposta` mantém `status()`, `cabecalhos()`, `dados()` e `totalBytes()`. `dados()` retorna uma cópia dos bytes. Para JSON, use `json()` para obter um `JsonNode` ou `decodificar(...)` para mapear o conteúdo para seu tipo. Respostas sem conteúdo têm um array vazio; não tente decodificá-las como JSON.

Falhas lançam `ErroMailcore`, uma exceção não verificada. `codigo()`, `status()` e `cabecalhos()` permitem classificar a falha. `mensagem()` pode conter informações privadas retornadas pela API; escolha cuidadosamente o que registrar ou mostrar ao usuário. A representação textual padrão da exceção omite a mensagem de negócio.

## Recursos disponíveis

O cliente oferece `emails()`, `lotes()`, `dominios()`, `chavesApi()`, `contatos()`, `segmentos()`, `topicos()`, `propriedadesContatos()`, `modelos()`, `webhooks()`, `campanhas()`, `automacoes()`, `eventos()`, `recebimento()`, `caixas()` e `cli()`.

Os recursos subordinados usam getters, como `contatos().exportacoes()`, `modelos().versoes()`, `caixas().conversas().emails()` e `cli().concessoes()`. Todos os métodos HTTP oferecem uma sobrecarga com `OpcoesRequisicao` no último argumento. Consultas e corpos opcionais também possuem conveniências sem esse argumento.

Consulte [uso.md](docs/uso.md) para configuração, paginação, composição, modelos, lotes, fluxo, idempotência e assinaturas. A [cobertura](docs/cobertura.md) lista as 151 operações e os limites do contrato.

## Transporte e execução

O transporte padrão usa HTTPS com validação de certificado e hostname, não segue redirecionamentos, não mantém cookies e não usa proxies do ambiente. O prazo padrão é **30 segundos**, a conexão TCP tem limite de **10 segundos** e cada resposta admite **16 MiB**, ajustáveis por configuração ou operação. Fluxos também respeitam o limite de bytes. HTTP exige opção explícita e só é aceito para loopback em testes.

O SDK não mantém uma fila de retentativas. Mutações usam corpos de uso único para impedir sua reemissão pelo motor HTTP. Leituras GET podem receber tratamento interno do OkHttp para HTTP 503 com `Retry-After: 0` ou HTTP 421 em conexões HTTP/2 coalescidas; não se garante uma única tentativa de rede para GET. Uma falha ou um cancelamento não comprova que o servidor rejeitou uma intenção.

Os testes de contrato exercitam as 151 operações com HTTP local real e dados sintéticos. Esses ensaios não medem capacidade de entrega SMTP nem desempenho em produção.
