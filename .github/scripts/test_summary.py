#!/usr/bin/env python3
"""Transforma os relatórios do Surefire no resumo do job (aba Summary do Actions).

Nunca falha o build: o gate de teste é o próprio Maven. Aqui só apresentamos.
"""

from __future__ import annotations

import os
import pathlib
import xml.etree.ElementTree as ET

RELATORIOS = pathlib.Path("target/surefire-reports")


def main() -> None:
    arquivos = sorted(RELATORIOS.glob("TEST-*.xml"))
    if not arquivos:
        escrever("### Testes\n\nNenhum relatório do Surefire encontrado.\n")
        return

    linhas = []
    total = falhas = erros = ignorados = 0
    duracao = 0.0

    for arquivo in arquivos:
        try:
            raiz = ET.parse(arquivo).getroot()
        except ET.ParseError:
            continue

        testes = int(raiz.get("tests", 0))
        falhou = int(raiz.get("failures", 0))
        errou = int(raiz.get("errors", 0))
        pulou = int(raiz.get("skipped", 0))
        tempo = float(raiz.get("time", 0) or 0)

        total += testes
        falhas += falhou
        erros += errou
        ignorados += pulou
        duracao += tempo

        classe = (raiz.get("name") or arquivo.stem).rsplit(".", 1)[-1]
        status = "✅" if falhou == 0 and errou == 0 else "❌"
        linhas.append(
            f"| {status} | `{classe}` | {testes} | {falhou} | {errou} | {pulou} | {tempo:.1f}s |"
        )

    passou = total - falhas - erros - ignorados
    cabecalho = "❌ Suíte com falhas" if (falhas or erros) else "✅ Suíte verde"

    corpo = [
        "### Testes",
        "",
        f"**{cabecalho}** — {passou}/{total} passaram, "
        f"{falhas} falha(s), {erros} erro(s), {ignorados} ignorado(s) em {duracao:.1f}s.",
        "",
        "| | Classe | Testes | Falhas | Erros | Ignorados | Tempo |",
        "| --- | --- | ---: | ---: | ---: | ---: | ---: |",
        *linhas,
        "",
    ]
    escrever("\n".join(corpo))

    for arquivo in arquivos:
        detalhar_falhas(arquivo)


def detalhar_falhas(arquivo: pathlib.Path) -> None:
    """Emite anotações do Actions para cada teste que falhou."""
    try:
        raiz = ET.parse(arquivo).getroot()
    except ET.ParseError:
        return

    for caso in raiz.iter("testcase"):
        problema = caso.find("failure") if caso.find("failure") is not None else caso.find("error")
        if problema is None:
            continue
        nome = f"{caso.get('classname')}.{caso.get('name')}"
        mensagem = (problema.get("message") or problema.get("type") or "sem mensagem").strip()
        mensagem = mensagem.replace("\n", " ")[:400]
        print(f"::error title=Teste falhou: {nome}::{mensagem}")


def escrever(texto: str) -> None:
    destino = os.environ.get("GITHUB_STEP_SUMMARY")
    if destino:
        with open(destino, "a", encoding="utf-8") as saida:
            saida.write(texto + "\n")
    print(texto)


if __name__ == "__main__":
    main()
