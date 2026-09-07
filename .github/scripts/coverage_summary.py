#!/usr/bin/env python3
"""Transforma o jacoco.xml no resumo do job (aba Summary do Actions).

Nunca falha o build: quem reprova cobertura baixa é o `jacoco:check` do Maven,
com o piso definido em `-Djacoco.line.minimum`.
"""

from __future__ import annotations

import os
import pathlib
import xml.etree.ElementTree as ET

RELATORIO = pathlib.Path("target/site/jacoco/jacoco.xml")

# Contador do JaCoCo -> rótulo em português.
CONTADORES = {
    "INSTRUCTION": "Instruções",
    "BRANCH": "Ramos",
    "LINE": "Linhas",
    "METHOD": "Métodos",
    "CLASS": "Classes",
}


def main() -> None:
    if not RELATORIO.exists():
        escrever("### Cobertura\n\nRelatório do JaCoCo não encontrado.\n")
        return

    raiz = ET.parse(RELATORIO).getroot()

    totais = []
    for contador in raiz.findall("counter"):
        tipo = contador.get("type", "")
        if tipo not in CONTADORES:
            continue
        perdidos = int(contador.get("missed", 0))
        cobertos = int(contador.get("covered", 0))
        total = perdidos + cobertos
        percentual = (cobertos / total * 100) if total else 0.0
        totais.append((CONTADORES[tipo], cobertos, total, percentual))

    piso = float(os.environ.get("COBERTURA_MINIMA", "0")) * 100
    linhas_pct = next((p for rotulo, _, _, p in totais if rotulo == "Linhas"), 0.0)
    veredito = "✅" if linhas_pct >= piso else "❌"

    corpo = [
        "### Cobertura",
        "",
        f"**{veredito} Linhas: {linhas_pct:.1f}%** (piso exigido: {piso:.0f}%)",
        "",
        "| Métrica | Coberto | Total | % |",
        "| --- | ---: | ---: | ---: |",
        *[f"| {rotulo} | {cob} | {tot} | {pct:.1f}% |" for rotulo, cob, tot, pct in totais],
        "",
        "<details><summary>Cobertura por pacote</summary>",
        "",
        "| Pacote | Linhas cobertas | % |",
        "| --- | ---: | ---: |",
    ]

    for pacote in sorted(raiz.findall("package"), key=lambda p: p.get("name", "")):
        contador = next(
            (c for c in pacote.findall("counter") if c.get("type") == "LINE"), None
        )
        if contador is None:
            continue
        perdidos = int(contador.get("missed", 0))
        cobertos = int(contador.get("covered", 0))
        total = perdidos + cobertos
        percentual = (cobertos / total * 100) if total else 0.0
        nome = (pacote.get("name") or "").replace("/", ".")
        corpo.append(f"| `{nome}` | {cobertos}/{total} | {percentual:.1f}% |")

    corpo += ["", "</details>", ""]
    escrever("\n".join(corpo))


def escrever(texto: str) -> None:
    destino = os.environ.get("GITHUB_STEP_SUMMARY")
    if destino:
        with open(destino, "a", encoding="utf-8") as saida:
            saida.write(texto + "\n")
    print(texto)


if __name__ == "__main__":
    main()
