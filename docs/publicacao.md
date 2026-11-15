# Publicação do SDK Java

A versão **0.1.0-alpha.1**, coordenadas **`br.com.mailcore:mailcore-java`**, está disponível no [Maven Central](https://central.sonatype.com/artifact/br.com.mailcore/mailcore-java/0.1.0-alpha.1). A publicação e o consumo público foram conferidos em **15/11/2026**.

Adicione a dependência ao POM da aplicação. O Maven utiliza o Central padrão, sem configuração de repositório adicional:

```xml
<dependency>
  <groupId>br.com.mailcore</groupId>
  <artifactId>mailcore-java</artifactId>
  <version>0.1.0-alpha.1</version>
</dependency>
```

O SDK requer Java 8 ou superior. O JAR principal mantém suas dependências separadas; Maven resolve o transporte HTTP e o suporte JSON pelo [POM publicado](https://repo.maven.apache.org/maven2/br/com/mailcore/mailcore-java/0.1.0-alpha.1/mailcore-java-0.1.0-alpha.1.pom).

| Referência | Valor |
|---|---|
| Tag anotada, imutável | [`v0.1.0-alpha.1`](https://github.com/mailcore-hq/mailcore-java/tree/v0.1.0-alpha.1) |
| Objeto da tag | `47254fc571093a0d3c99fcf8a60f91d0ccfa5d4a` |
| Commit do pacote publicado | `974b828d8010cd547c68f742b880ba839230625a` |
| Fingerprint da chave pública de assinatura | `D1EAFE26A1057757C7A118689E5F1DCA3EB81B12` |

O conteúdo da tag e dos artefatos publicados permanece preservado. O registro documental posterior em `main` não substitui o pacote no Central.

## Integridade e assinaturas

Os quatro artefatos e suas quatro assinaturas `.asc` foram baixados por HTTPS do Maven Central e corresponderam byte a byte ao bundle publicado. As assinaturas foram verificadas em um container novo, sem rede, com chaveiro inicialmente vazio e somente a chave pública cujo fingerprint consta acima. A obtenção independente dessa chave pelo servidor HKPS já havia sido comprovada.

| Arquivo | SHA-256 |
|---|---|
| `mailcore-java-0.1.0-alpha.1.jar` | `3b3cc18698f57fb7d892bf2b24ef52d819c2d774c8d2a2bbd0253b494d9ffa35` |
| `mailcore-java-0.1.0-alpha.1-sources.jar` | `fc7f8890bb62bb0b02ccbd14907c2883fe14251d23de4b6bb002ed3e495f874b` |
| `mailcore-java-0.1.0-alpha.1-javadoc.jar` | `541113ea2326818d8e0ba31e02bb3163926b2ac5180fdc6b1e3a61fddc592c91` |
| `mailcore-java-0.1.0-alpha.1.pom` | `fa6d5527b640152d87aaa939896f4564b34f8c2928c60f28dd87eb0b766b54cf` |

Os 48 fontes do JAR de código-fonte foram comparados ao `src/main/java` do commit público. As 65 classes do JAR principal usam bytecode 52, compatível com Java 8.

## Consumo público confirmado

O consumidor utilizou **Maven 3.9.16 e Java 17.0.21** em Docker, fora do checkout e com cache Maven inicialmente vazio. O POM não declarou repositórios adicionais, os settings não continham credenciais ou mirrors e a resolução utilizou o Central padrão com HTTPS e checksums estritos. O JAR e o POM instalados corresponderam aos hashes acima; os fontes do SDK não foram compilados dentro do consumidor.

O [consumidor existente](../testes/consumidor/ConsumidorInstalado.java) aprovou **30 recursos, 151 operações, 155 requisições sintéticas e três callbacks**, além de pedidos tipados, precisão JSON, User-Agent, assinaturas de webhook, proteção de cabeçalhos e cancelamento. O exemplo de envio foi compilado para Java 8, sem ser executado.

A rede foi usada para resolver e baixar os artefatos. As operações do SDK usaram transporte HTTP sintético, sem acesso à API real ou envio de emails. Esse ensaio não mede entrega SMTP nem desempenho sob carga. A matriz de desenvolvimento em seis versões Java está registrada separadamente em [verificação](verificacao.md).

Containers, consumidor, caches e arquivos temporários foram removidos após a verificação; os artefatos públicos e os registros de integridade foram preservados.
