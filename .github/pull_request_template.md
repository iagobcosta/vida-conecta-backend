## O que muda

<!-- Uma frase sobre o comportamento que passa a existir (ou deixa de existir). -->

## Por quê

<!-- Contexto, issue relacionada, decisão de produto. -->

## Como testar

<!-- Passos ou requisições que provam a mudança funcionando. -->

## Checklist

- [ ] Testes cobrindo o caminho feliz e ao menos um caso de erro
- [ ] Migration nova é aditiva e não altera arquivo `V*.sql` já mergeado
- [ ] Fronteiras entre módulos respeitadas (acesso só via `api/` do outro módulo)
- [ ] Nenhum segredo, chave ou credencial no diff
- [ ] README/OpenAPI atualizados se o contrato da API mudou
