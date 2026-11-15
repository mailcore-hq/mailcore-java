#!/usr/bin/env python3
"""Instala o JAR local em consumidor Maven separado, totalmente offline."""

import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path

from preparar_distribuicao import GRUPO, PACOTE, VERSAO, COORDENADA, inventariar_repositorio

SDK = Path(__file__).resolve().parents[1]
PERFIS = {"8", "11", "17", "21", "25", "27"}


def exigir(condicao):
    if not condicao:
        raise ValueError("O contrato de instalação não foi satisfeito.")


def executar(argumentos, ambiente, pasta, prazo=300):
    retorno = subprocess.run(argumentos, cwd=pasta, env=ambiente, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=prazo, check=False)
    exigir(retorno.returncode == 0 and len(retorno.stdout) <= 16 * 1024 * 1024)
    return retorno.stdout


def salvar(destino, documento):
    destino.parent.mkdir(parents=True, exist_ok=True)
    with os.fdopen(os.open(destino, os.O_WRONLY | os.O_CREAT | os.O_TRUNC | os.O_NOFOLLOW, 0o644), "w") as arquivo:
        json.dump(documento, arquivo, ensure_ascii=False, indent=2)
        arquivo.write("\n")


def pom_consumidor():
    return '''<project xmlns="http://maven.apache.org/POM/4.0.0"><modelVersion>4.0.0</modelVersion>
<groupId>br.com.mailcore.ensaio</groupId><artifactId>consumidor-instalado</artifactId><version>0.0.0</version>
<properties><project.build.sourceEncoding>UTF-8</project.build.sourceEncoding><maven.compiler.release>8</maven.compiler.release><maven.test.skip>true</maven.test.skip></properties>
<dependencies><dependency><groupId>br.com.mailcore</groupId><artifactId>mailcore-java</artifactId><version>0.1.0-alpha.1</version></dependency></dependencies>
<build><plugins>
<plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.16.0</version></plugin>
<plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-resources-plugin</artifactId><version>3.3.1</version></plugin>
<plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-jar-plugin</artifactId><version>3.4.2</version></plugin>
<plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-surefire-plugin</artifactId><version>3.5.4</version></plugin>
<plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-dependency-plugin</artifactId><version>3.8.1</version></plugin>
</plugins></build></project>'''


def instalar(artefatos, perfil):
    manifesto = json.loads((artefatos / "manifesto.json").read_bytes())
    exigir(manifesto["grupo"] == GRUPO and manifesto["pacote"] == PACOTE and manifesto["versao"] == VERSAO)
    exigir(manifesto["bytecode_major"] == 52 and manifesto["licenca_sdk_nos_tres_jars"] and manifesto["dependencias_fora_jar_sdk"])
    for item in manifesto["artefatos"].values():
        caminho = (artefatos / item["arquivo"]).resolve()
        exigir(caminho.is_relative_to(artefatos) and hashlib.sha256(caminho.read_bytes()).hexdigest() == item["sha256"])
    digest, arquivos_cache, licencas = inventariar_repositorio(artefatos / "maven-repositorio")
    exigir(digest == manifesto["sha256_repositorio"] and arquivos_cache == manifesto["arquivos_repositorio"] and licencas == manifesto["licencas_notices_repositorio"])
    with tempfile.TemporaryDirectory(prefix="mailcore-java-consumidor-") as temporario:
        pasta = Path(temporario)
        repositorio = Path(os.environ.get("MAILCORE_REPOSITORIO_CONSUMIDOR", str(pasta / "maven")))
        repositorio.mkdir(parents=True, exist_ok=True)
        # Cache de uso exclusivo do ensaio, sem settings/credenciais do host.
        shutil.copytree(artefatos / "maven-repositorio", repositorio, dirs_exist_ok=True)
        shutil.copytree(artefatos / "repo", repositorio, dirs_exist_ok=True)
        classes = pasta / "src/main/java/br/com/mailcore/consumidor"
        classes.mkdir(parents=True)
        shutil.copyfile(SDK / "testes/consumidor/ConsumidorInstalado.java", classes / "ConsumidorInstalado.java")
        exemplo = artefatos / "exemplos/envio/EnviarEmail.java"
        exigir(hashlib.sha256(exemplo.read_bytes()).hexdigest() == manifesto["exemplos"]["envio/EnviarEmail.java"])
        shutil.copyfile(exemplo, pasta / "src/main/java/EnviarEmail.java")
        (pasta / "pom.xml").write_text(pom_consumidor())
        configuracao = pasta / "settings.xml"
        configuracao.write_text('<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"><offline>true</offline><interactiveMode>false</interactiveMode></settings>')
        ambiente = os.environ.copy()
        ambiente.update({"MAVEN_ARGS": "", "MAVEN_CONFIG": str(pasta / "configuracao"), "MAVEN_OPTS": "-Xmx384m -Djava.awt.headless=true", "JAVA_TOOL_OPTIONS": "", "JDK_JAVA_OPTIONS": ""})
        versao = subprocess.run(["java", "-version"], env=ambiente, stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=30, check=False)
        exigir(versao.returncode == 0)
        texto = (versao.stdout + versao.stderr).decode("utf-8", errors="strict")
        encontrada = re.search(r'version "(?:1\.)?(\d+)(?:[.\-+"_])', texto)
        exigir(encontrada is not None and encontrada.group(1) in PERFIS)
        perfil_real = encontrada.group(1)
        exigir(perfil is None or perfil == perfil_real)
        comando = ["mvn", "-B", "-ntp", "-o", "--settings", str(configuracao), "-Dmaven.repo.local=" + str(repositorio)]
        executar([*comando, "package", "dependency:build-classpath", "-DincludeScope=compile", "-Dmdep.outputFile=" + str(pasta / "classpath.txt")], ambiente, pasta)
        classe_exemplo = (pasta / "target/classes/EnviarEmail.class").read_bytes()
        exigir(classe_exemplo[:4] == b"\xca\xfe\xba\xbe" and int.from_bytes(classe_exemplo[6:8], "big") == 52)
        dependencias = (pasta / "classpath.txt").read_text().strip().split(os.pathsep)
        exigir(dependencias and all(Path(nome).resolve().is_relative_to(repositorio.resolve()) for nome in dependencias))
        jar_sdk = repositorio / COORDENADA / (PACOTE + "-" + VERSAO + ".jar")
        exigir(str(jar_sdk) in dependencias and not jar_sdk.resolve().is_relative_to(SDK))
        exigir(hashlib.sha256(jar_sdk.read_bytes()).hexdigest() == manifesto["artefatos"]["jar"]["sha256"])
        classpath = str(pasta / "target/classes") + os.pathsep + os.pathsep.join(dependencias)
        saida = executar(["java", "-cp", classpath, "br.com.mailcore.consumidor.ConsumidorInstalado"], ambiente, pasta)
        resultado = json.loads(saida)
        exigir(resultado.get("resultado") == "aprovado" and resultado.get("recursos") == 30 and resultado.get("metodos") == 151 and resultado.get("callbacks_fluxo") == 3)
        exigir(resultado.get("requisicoes_http") == 155 and resultado.get("origem_jar") and resultado.get("rede_real") is False)
        resultado.update({
            "data_utc": datetime.now(timezone.utc).isoformat(), "perfil": perfil_real,
            "grupo": GRUPO, "pacote": PACOTE, "versao": VERSAO,
            "sha256_jar": manifesto["artefatos"]["jar"]["sha256"], "sha256_repositorio": digest,
            "bytecode_major": 52, "fontes_javadoc_pom_licencas_preservados": True,
            "dependencias_fora_jar_sdk": True, "modulo_fora_checkout": True,
            "exemplo_compilado_sem_executar": True,
            "maven_offline": True, "instalacao_host": False, "publicado": False,
        })
        return resultado


def agregar(pasta):
    resultados = [json.loads(caminho.read_bytes()) for caminho in sorted(pasta.glob("instalacao-java-*.json"))]
    exigir(len(resultados) == len(PERFIS) and {item.get("perfil") for item in resultados} == PERFIS)
    exigir(all(item.get("resultado") == "aprovado" and item.get("metodos") == 151 and item.get("requisicoes_http") == 155 and item.get("callbacks_fluxo") == 3 for item in resultados))
    exigir(len({item.get("sha256_jar") for item in resultados}) == 1 and len({item.get("sha256_repositorio") for item in resultados}) == 1)
    return {"resultado": "aprovado", "data_utc": datetime.now(timezone.utc).isoformat(), "grupo": GRUPO, "pacote": PACOTE, "versao": VERSAO, "publicado": False, "perfis": resultados}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--artefatos", type=Path, default=Path("/pacotes"))
    parser.add_argument("--relatorio", type=Path)
    parser.add_argument("--perfil", choices=sorted(PERFIS))
    parser.add_argument("--agregar", type=Path)
    opcoes = parser.parse_args()
    destino = opcoes.relatorio
    try:
        if opcoes.agregar:
            resultado = agregar(opcoes.agregar.resolve())
            destino = destino or opcoes.agregar / "instalacao-local.json"
        else:
            exigir(destino is not None)
            resultado = instalar(opcoes.artefatos.resolve(), opcoes.perfil)
    except Exception:
        resultado = {"resultado": "falhou", "etapa": "instalacao", "perfil": opcoes.perfil, "falha": "O consumidor instalado não satisfez o contrato; detalhes omitidos."}
    if destino is not None:
        try:
            salvar(destino, resultado)
        except Exception:
            resultado = {"resultado": "falhou", "etapa": "relatorio", "falha": "Não foi possível registrar o ensaio local."}
    print(json.dumps(resultado, ensure_ascii=False))
    return 0 if resultado["resultado"] == "aprovado" else 1


if __name__ == "__main__":
    sys.exit(main())
