#!/usr/bin/env python3
"""Compila em Docker e executa os mesmos testes, offline, em cada JDK suportado."""

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

SDK = Path(__file__).resolve().parents[1]
IMAGENS = json.loads((SDK / "testes/imagens.json").read_text())


def executar(argumentos, registro=None):
    retorno = subprocess.run(argumentos, cwd=SDK, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    saida = retorno.stdout.decode("utf-8", errors="replace")
    if registro:
        registro.write_text(saida)
    if retorno.returncode:
        print(saida[-16000:], file=sys.stderr)
        raise RuntimeError("Falha na verificação; veja o registro informado.")
    return saida


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--perfis", nargs="+", choices=list(IMAGENS), default=list(IMAGENS))
    parser.add_argument("--sem-build", action="store_true", help="Usa imagens já compiladas desta árvore.")
    argumentos = parser.parse_args()
    pasta = SDK / "dist/verificacao"
    pasta.mkdir(parents=True, exist_ok=True)
    executar([sys.executable, "scripts/gerar_recursos.py", "--verificar"])
    resultados = []
    for perfil in argumentos.perfis:
        imagem = "mailcore/java-testes:" + perfil
        if not argumentos.sem_build:
            print("Compilando Java " + perfil + "...", flush=True)
            executar(["docker", "build", "--build-arg", "IMAGEM_JAVA=" + IMAGENS[perfil], "-t", imagem, "."], pasta / ("build-" + perfil + ".log"))
        print("Verificando Java " + perfil + " sem rede externa...", flush=True)
        saida = executar(["docker", "run", "--rm", "--network", "none", "--read-only",
                          "--cap-drop", "ALL", "--security-opt", "no-new-privileges", "--memory", "1536m",
                          "--tmpfs", "/tmp:rw,exec,nosuid,size=384m", imagem], pasta / ("java-" + perfil + ".log"))
        testes = re.search(r"OK \((\d+) tests?\)", saida)
        if not testes:
            raise RuntimeError("O JUnit não confirmou a conclusão da suíte.")
        versao = executar(["docker", "run", "--rm", "--network", "none", "--read-only", imagem, "java", "-version"])
        resultado = {"perfil": perfil, "imagem_base": IMAGENS[perfil], "java": versao.splitlines()[0],
                     "testes": int(testes.group(1)), "falhas": 0, "rede": "none"}
        resultados.append(resultado)
        print("Java " + perfil + ": " + testes.group(1) + " testes aprovados.", flush=True)
    arquivo = pasta / "matriz.json"
    anterior = json.loads(arquivo.read_text()) if arquivo.exists() else []
    por_perfil = {r["perfil"]: r for r in anterior}
    por_perfil.update({r["perfil"]: r for r in resultados})
    arquivo.write_text(json.dumps(sorted(por_perfil.values(), key=lambda r: int(r["perfil"])), ensure_ascii=False, indent=2) + "\n")


if __name__ == "__main__":
    try:
        main()
    except (RuntimeError, OSError) as falha:
        print(str(falha), file=sys.stderr)
        sys.exit(1)
