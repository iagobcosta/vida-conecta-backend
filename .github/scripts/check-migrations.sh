#!/usr/bin/env bash
# Regras de migration que o Flyway só reclamaria em tempo de execução:
#   1. nome no padrão V<versao>__descricao.sql (ou R__descricao.sql);
#   2. nenhuma versão duplicada — o Flyway aborta o boot inteiro nesse caso.
set -euo pipefail

DIRETORIO="src/main/resources/db/migration"
falhou=0

if [ ! -d "$DIRETORIO" ]; then
	echo "::error::Diretório de migrations não encontrado: $DIRETORIO"
	exit 1
fi

shopt -s nullglob
arquivos=("$DIRETORIO"/*.sql)
shopt -u nullglob

if [ ${#arquivos[@]} -eq 0 ]; then
	echo "::error::Nenhuma migration em $DIRETORIO"
	exit 1
fi

versoes=""
for arquivo in "${arquivos[@]}"; do
	nome=$(basename "$arquivo")
	case "$nome" in
	V[0-9]*__*.sql)
		versoes="${versoes}${nome%%__*}"$'\n'
		;;
	R__*.sql) ;;
	*)
		echo "::error file=$arquivo::'$nome' fora do padrão Flyway (V<versao>__descricao.sql ou R__descricao.sql)"
		falhou=1
		;;
	esac
done

duplicadas=$(printf '%s' "$versoes" | sort | uniq -d)
if [ -n "$duplicadas" ]; then
	echo "::error::Versões de migration duplicadas: $(printf '%s' "$duplicadas" | tr '\n' ' ')"
	falhou=1
fi

if [ "$falhou" -eq 0 ]; then
	echo "${#arquivos[@]} migration(s) validada(s) em $DIRETORIO."
fi

exit "$falhou"
