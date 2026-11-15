# Verificação

O projeto compila para Java 8 com JDK 17 e executa o mesmo bytecode em Java 8, 11, 17, 21, 25 e 27. As imagens base estão fixadas por digest em `testes/imagens.json`. O compilador usa `--release 8`, `-Xlint:all` e `-Werror`; o pacote contém JAR principal, fontes, Javadoc e POM.

Em 14/11/2026, **188 testes passaram em cada um dos seis perfis**, totalizando 1.128 execuções sem falhas. O [relatório da matriz](matriz-java.json) registra as versões exatas. A [integração com API e PostgreSQL](integracao-api.json) também foi aprovada nos dez grupos, com quatro fluxos, oito leituras concorrentes, TLS verificado e limpeza confirmada.

## Matriz em Docker

```bash
python3 scripts/verificar.py
```

JDK e Maven permanecem nos contêineres. A compilação baixa dependências públicas fixadas no POM. A execução dos testes usa `--network none`, sistema de arquivos somente leitura e diretório temporário próprio. O controlador grava os resultados em `dist/verificacao/matriz.json` e os registros individuais na mesma pasta.

A suíte reúne 151 contratos de operação por HTTP local, 24 testes de protocolo HTTPS, oito de assinatura e cinco de objetos públicos. Os casos incluem:

- Métodos, caminhos, escape de identificadores, filtros, corpos, opções e downloads das 151 operações.
- Certificados e hostname, ausência de proxy automático, pooling, concorrência e propriedade do cliente HTTP.
- Corpo mutante de uso único, incluindo POST vazio e HTTP 503 com `Retry-After: 0`; conexão interrompida após receber a intenção.
- Limites de bytes e cabeçalhos, respostas truncadas, codificações inesperadas, JSON/UTF-8 inválidos e precisão numérica.
- Downloads progressivos de 8 MiB com blocos próprios, cancelamento, prazo total e encerramento durante uma leitura.
- HMAC dos bytes originais, janela temporal, rotação de assinatura, cabeçalhos ambíguos e limites do corpo.
- Cópias defensivas, snapshots dos pedidos e sanitização de exceções do cliente HTTP personalizado.

A fixture sintética negocia HTTP/1.1. O caso HTTP 421 dessa fixture não comprova coalescência HTTP/2. GET pode receber follow-ups internos do transporte; o SDK não promete uma única tentativa para leituras.

## Distribuição e consumidor instalado

Após construir a imagem, prepare os artefatos locais:

```bash
mkdir -p dist/pacotes
docker run --rm --network none --read-only \
  --tmpfs /tmp:rw,exec,nosuid,size=384m \
  --mount type=bind,src="$(pwd)/dist/pacotes",dst=/pacotes \
  mailcore/java-testes:17 python3 scripts/preparar_distribuicao.py
```

`dist/pacotes/repo` é um repositório Maven local com o SDK. `maven-repositorio` contém dependências públicas e plugins necessários ao ensaio offline, preservando seus avisos e licenças. Nenhum arquivo de credenciais ou configuração Maven do host é incorporado. O manifesto SHA-256 inventaria os artefatos e o cache; os três JARs incluem a licença MIT do SDK.

O [registro de distribuição](distribuicao.json) contém os checksums dos quatro artefatos. O JAR principal contém 65 classes, bytecode 52, e seu SHA-256 é idêntico nas seis imagens finais de verificação.

Para provar a instalação em um projeto consumidor fora do checkout:

```bash
mkdir -p dist/instalacao
docker run --rm --network none --read-only \
  --tmpfs /tmp:rw,exec,nosuid,size=512m \
  --mount type=bind,src="$(pwd)/dist/pacotes",dst=/pacotes,readonly \
  --mount type=bind,src="$(pwd)/dist/instalacao",dst=/relatorios \
  mailcore/java-testes:17 python3 scripts/instalar_consumidor.py \
  --perfil 17 --relatorio /relatorios/instalacao-java-17.json
```

Repita com as imagens e perfis 8, 11, 21, 25 e 27. O consumidor resolve o POM por Maven offline, compila separadamente, carrega as classes a partir do JAR instalado e usa um interceptor sintético para exercitar os 30 recursos, 151 operações e três callbacks de fluxo. Esse ensaio verifica empacotamento; os contratos HTTP reais pertencem à suíte de protocolo e à integração abaixo.

A [instalação local foi aprovada nos seis perfis](instalacao-local.json), usando o mesmo JAR e o mesmo inventário de dependências. O exemplo `EnviarEmail.java` foi compilado separadamente em cada perfil, sem executar envios.

## API isolada

`testes/controlar_api.py` usa um checkout do backend, a imagem local de runtime da API e o compilador Rust fixado no próprio controlador. Ele cria rede interna, PostgreSQL e API exclusivos, com TLS e nomes de serviço Docker. Não utiliza worker, SMTP, portas publicadas ou banco de produção.

O controlador precisa acessar o daemon Docker para criar e remover seus recursos. Execute-o apenas com o checkout e as imagens locais de teste, passando `--backend`, `--cache-cargo` e `--imagem-runtime-api` conforme o ambiente. O relatório `docs/integracao-api.json` identifica o binário e o fingerprint dos fontes, os cenários executados e a confirmação da limpeza.

Exemplo para mantenedores com acesso ao backend e cache público Cargo previamente preparado:

```bash
# Execute na raiz deste SDK, com o backend em ../../mailcore.
diretorio_sdk="$(pwd)"
diretorio_backend="$(cd ../../mailcore && pwd)"
diretorio_cache_cargo="${CARGO_HOME:-$HOME/.cargo}/registry"

docker run --rm --stop-timeout 30 --network none \
  --mount type=bind,src=/var/run/docker.sock,dst=/var/run/docker.sock \
  --mount "type=bind,src=$diretorio_backend,dst=$diretorio_backend,readonly" \
  --mount "type=bind,src=$diretorio_sdk,dst=$diretorio_sdk" \
  --workdir "$diretorio_sdk" \
  --entrypoint python3 mailcore/java-testes:17 testes/controlar_api.py \
  --backend "$diretorio_backend" \
  --cache-cargo "$diretorio_cache_cargo"
```

O cache contém somente artefatos públicos; credenciais Cargo não são montadas. O compilador filho recebe os fontes somente para leitura e usa `cargo build --offline --locked`, com escrita restrita ao diretório de compilação. Os caminhos precisam permanecer iguais no host e no controlador porque o daemon Docker resolve os binds dos contêineres filhos.

Um [ensaio separado de interrupção intencional](integracao-api-interrupcao.json) enviou SIGTERM após confirmar PostgreSQL, guardião, rede interna e volume temporário no daemon. O controlador encerrou o ensaio e removeu todos esses recursos; a listagem posterior confirmou zero remanescentes. O resultado de operação interrompida nesse relatório é esperado.

Os dez grupos cobrem autenticação, idempotência, revisões, envios compostos, modelos, contatos, exportações/importações, recebimento, caixas e webhooks. Incluem quatro fluxos de download e oito leituras concorrentes com uma instância compartilhada. Isso verifica aceitação e persistência na API; não mede entrega SMTP nem capacidade de produção.

O SDK continua em versão alpha local. Não há publicação ou deploy nesses comandos.
