#!/usr/bin/env python3
"""Prepara JAR/POM/fontes/Javadoc e repositório Maven offline, sem publicação."""

import argparse
import hashlib
import json
import re
import shutil
import sys
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

SDK = Path(__file__).resolve().parents[1]
GRUPO = "br.com.mailcore"
PACOTE = "mailcore-java"
VERSAO = "0.1.0-alpha.1"
COORDENADA = Path(*GRUPO.split(".")) / PACOTE / VERSAO


def exigir(condicao):
    if not condicao:
        raise ValueError("O contrato de distribuição não foi satisfeito.")


def sha256(conteudo):
    return hashlib.sha256(conteudo).hexdigest()


def inventariar_repositorio(pasta):
    digest = hashlib.sha256()
    arquivos = {}
    licencas = {}
    for caminho in sorted(pasta.rglob("*")):
        exigir(not caminho.is_symlink())
        if not caminho.is_file():
            continue
        nome = caminho.relative_to(pasta).as_posix()
        conteudo = caminho.read_bytes()
        arquivos[nome] = sha256(conteudo)
        digest.update(nome.encode() + b"\0" + conteudo)
        if nome.endswith(".jar"):
            with zipfile.ZipFile(caminho) as arquivo:
                licencas[nome] = [entrada for entrada in arquivo.namelist() if entrada.rsplit("/", 1)[-1].upper().startswith(("LICENSE", "LICENCE", "NOTICE", "COPYING", "COPYRIGHT"))]
    exigir(any(nome.endswith(".jar") for nome in arquivos) and any(nome.endswith(".pom") for nome in arquivos))
    return digest.hexdigest(), arquivos, licencas


def copiar_repositorio(origem, destino):
    exigir(origem.is_dir() and not origem.is_symlink())
    destino.mkdir(parents=True, exist_ok=True)
    for caminho in sorted(origem.rglob("*")):
        exigir(not caminho.is_symlink())
        if not caminho.is_file():
            continue
        nome = caminho.name
        # Apenas artefatos/descritores públicos e controles de resolução Maven.
        if not (nome.endswith((".jar", ".pom", ".module", ".sha1", ".sha256", ".sha512", ".md5"))
                or nome.startswith("maven-metadata") and nome.endswith(".xml")
                or nome in ("_remote.repositories", "resolver-status.properties")):
            continue
        alvo = destino / caminho.relative_to(origem)
        alvo.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(caminho, alvo)


def validar_jar(caminho, tipo, licenca):
    classes = 0
    licenca_encontrada = False
    with zipfile.ZipFile(caminho) as arquivo:
        exigir(arquivo.testzip() is None)
        nomes = arquivo.namelist()
        exigir(len(nomes) == len(set(nomes)))
        for nome in nomes:
            exigir(not nome.startswith("/") and "\\" not in nome and ".." not in nome.split("/"))
            exigir(not any(parte in ("testes", "target", ".git") for parte in nome.split("/")))
            exigir("/integracao/" not in nome and "IntegracaoApi" not in nome and "ConsumidorInstalado" not in nome)
            if nome.endswith("/"):
                continue
            conteudo = arquivo.read(nome)
            licenca_encontrada |= conteudo == licenca
            if nome.endswith(".class"):
                exigir(tipo == "jar" and nome.startswith("br/com/mailcore/sdk/"))
                exigir(conteudo[:4] == b"\xca\xfe\xba\xbe" and int.from_bytes(conteudo[6:8], "big") == 52)
                classes += 1
        if tipo == "jar":
            exigir(classes >= 30 and "br/com/mailcore/sdk/Cliente.class" in nomes)
            propriedades = arquivo.read("META-INF/maven/br.com.mailcore/mailcore-java/pom.properties").decode()
            exigir("artifactId=" + PACOTE in propriedades and "version=" + VERSAO in propriedades and "groupId=" + GRUPO in propriedades)
        elif tipo == "fontes":
            exigir("br/com/mailcore/sdk/Cliente.java" in nomes)
        elif tipo == "javadoc":
            exigir("index.html" in nomes)
    exigir(licenca_encontrada)
    return classes


def preparar(destino, repositorio):
    licenca = (SDK / "LICENSE").read_bytes()
    pom = (SDK / "pom.xml").read_bytes()
    raiz = ET.fromstring(pom)
    ns = {"m": "http://maven.apache.org/POM/4.0.0"}
    exigir(raiz.findtext("m:groupId", namespaces=ns) == GRUPO and raiz.findtext("m:artifactId", namespaces=ns) == PACOTE and raiz.findtext("m:version", namespaces=ns) == VERSAO)
    destino.mkdir(parents=True, exist_ok=True)
    repo_sdk = destino / "repo" / COORDENADA
    repo_sdk.mkdir(parents=True, exist_ok=True)
    artefatos = {}
    classes = 0
    for tipo, sufixo in (("jar", ".jar"), ("fontes", "-sources.jar"), ("javadoc", "-javadoc.jar"), ("pom", ".pom")):
        nome = PACOTE + "-" + VERSAO + sufixo
        if tipo == "pom":
            conteudo = pom
        else:
            arquivo = SDK / "target" / nome
            classes += validar_jar(arquivo, tipo, licenca)
            conteudo = arquivo.read_bytes()
        (repo_sdk / nome).write_bytes(conteudo)
        for algoritmo in ("sha256", "sha512", "sha1", "md5"):
            # MD5/SHA1 são somente sidecars de compatibilidade; a evidência usa SHA256.
            soma = hashlib.new(algoritmo, conteudo).hexdigest()
            (repo_sdk / (nome + "." + algoritmo)).write_text(soma + "\n")
        artefatos[tipo] = {"arquivo": (Path("repo") / COORDENADA / nome).as_posix(), "sha256": sha256(conteudo), "bytes": len(conteudo)}
    (repo_sdk / "_remote.repositories").write_text("# Artefatos preparados localmente; nenhum registry foi alterado.\n" + "".join(Path(item["arquivo"]).name + ">=\n" for item in artefatos.values()))
    copiar_repositorio(repositorio, destino / "maven-repositorio")
    digest, arquivos_cache, licencas = inventariar_repositorio(destino / "maven-repositorio")
    manifesto = {
        "resultado": "preparado", "grupo": GRUPO, "pacote": PACOTE, "versao": VERSAO,
        "artefatos": artefatos, "classes_sdk": classes, "bytecode_major": 52,
        "sha256_repositorio": digest, "arquivos_repositorio": arquivos_cache,
        "licencas_notices_repositorio": licencas, "licenca_sdk_nos_tres_jars": True,
        "dependencias_fora_jar_sdk": True, "repositorio_inclui_plugins_testes": True, "publicado": False,
    }
    (destino / "LICENSE").write_bytes(licenca)
    exemplo = SDK / "exemplos/envio/EnviarEmail.java"
    exigir(exemplo.is_file())
    (destino / "exemplos/envio").mkdir(parents=True, exist_ok=True)
    shutil.copyfile(exemplo, destino / "exemplos/envio/EnviarEmail.java")
    manifesto["exemplos"] = {"envio/EnviarEmail.java": sha256(exemplo.read_bytes())}
    for nome in ("README.md", "SECURITY.md", "CONTRIBUTING.md", "CHANGELOG.md"):
        exigir((SDK / nome).is_file())
        shutil.copyfile(SDK / nome, destino / nome)
    for caminho in sorted((SDK / "docs").rglob("*.md")):
        exigir(not caminho.is_symlink())
        alvo = destino / "docs" / caminho.relative_to(SDK / "docs")
        alvo.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(caminho, alvo)
    (destino / "manifesto.json").write_text(json.dumps(manifesto, ensure_ascii=False, indent=2) + "\n")
    return {"resultado": "preparado", "pacote": PACOTE, "versao": VERSAO, "classes_sdk": classes, "bytecode_major": 52, "sha256_jar": artefatos["jar"]["sha256"], "arquivos_repositorio": len(arquivos_cache)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--destino", type=Path, default=Path("/pacotes"))
    parser.add_argument("--repositorio", type=Path, default=Path("/opt/maven-repositorio"))
    opcoes = parser.parse_args()
    try:
        resultado = preparar(opcoes.destino.resolve(), opcoes.repositorio.resolve())
    except Exception:
        resultado = {"resultado": "falhou", "etapa": "distribuicao", "falha": "Não foi possível preparar os artefatos locais; detalhes omitidos."}
    print(json.dumps(resultado, ensure_ascii=False))
    return 0 if resultado["resultado"] == "preparado" else 1


if __name__ == "__main__":
    sys.exit(main())
