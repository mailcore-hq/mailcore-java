# Contribuir

O código e a documentação usam português do Brasil. Os nomes dos campos do protocolo permanecem conforme o contrato HTTP. Não introduza endpoints por inferência: confira as rotas e DTOs públicos do Mailcore antes de alterar um recurso.

Cada SDK possui repositório próprio. Para verificar esta biblioteca, Docker e Python 3 do controlador são suficientes; JDK e Maven são executados nos contêineres:

```bash
python3 scripts/gerar_recursos.py --verificar
python3 scripts/verificar.py
```

Para uma versão durante o desenvolvimento, use `python3 scripts/verificar.py --perfis 17`. O build compila com `--release 8`, `-Xlint:all` e `-Werror`. Os testes executam sem rede externa, com HTTPS e credenciais sintéticas. Não configure chaves reais nesses ensaios.

Altere o catálogo em `scripts/gerar_recursos.py` para regenerar recursos. A fixture de contratos é independente do gerador e deve acompanhar alterações comprovadas no backend. Cubra diferenças observáveis do protocolo, erros e limites; evite testes que apenas repitam a implementação.

Consulte [a verificação](docs/verificacao.md) para distribuição e integração com a API isolada. Os ensaios não precisam de serviços de produção, portas públicas, worker ou SMTP. Mudanças devem preservar privacidade, concorrência, limites de memória e a propriedade do cliente HTTP injetado.

Esta etapa não publica artefatos em registros públicos.
