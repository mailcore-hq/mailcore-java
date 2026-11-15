# Segurança

Use a biblioteca no servidor e mantenha a chave de API em um ambiente protegido ou fornecedor de credenciais. Não inclua chaves em código, URLs, mensagens de erro públicas ou repositórios. Use escopos mínimos e uma chave por integração.

O cliente padrão exige HTTPS, verifica certificados e hostname, desativa cookies, proxies e redirecionamentos. HTTP só é permitido em loopback, mediante opção explícita de teste. Um cliente HTTP injetado permanece sob responsabilidade da aplicação, incluindo suas políticas TLS e interceptores.

Não registre o corpo de pedidos, respostas, eventos ou `ErroMailcore.mensagem()` sem tratamento: podem conter dados pessoais. As representações textuais dos objetos públicos omitem seu conteúdo. O SDK não persiste dados, chaves ou eventos; armazenamento e criptografia em repouso pertencem à aplicação.

Verifique a assinatura antes de interpretar um webhook e deduplique o identificador em armazenamento durável. A verificação de assinatura não é um controle de repetição persistente.

Comunique vulnerabilidades ao responsável pelo projeto em um canal privado. Informe a versão, um exemplo mínimo com dados sintéticos e o comportamento esperado. Não publique chaves, dados de clientes ou provas que dependam de sistemas de terceiros.
