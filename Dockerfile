ARG IMAGEM_JAVA=eclipse-temurin:17-jdk-noble@sha256:1bedb04434bd275ef072355e9caec2a22be5240fbe82ad51a59323f2bbcab4a6
FROM maven:3.9.16-eclipse-temurin-17@sha256:1a352420f7aba21f5ad08df31bab55f74c013fb491f1ae8ab1dd7ff9ed698584 AS ferramentas
RUN apt-get update && apt-get install -y --no-install-recommends python3 ca-certificates openssl unzip zip && rm -rf /var/lib/apt/lists/*
COPY --from=docker:29-cli@sha256:5a7af0dcb0e46c803efe9b45371dd21cb500e5b4453250394ac6f06d49b6e6f1 /usr/local/bin/docker /usr/local/bin/docker
WORKDIR /sdk
ENV MAVEN_OPTS="-Xmx768m" MAVEN_CONFIG=/tmp/maven

FROM ferramentas AS dependencias
COPY pom.xml ./
RUN mvn -B -ntp -Dmaven.repo.local=/opt/maven-repositorio dependency:go-offline

FROM dependencias AS testes
COPY . .
RUN mvn -B -ntp -o -Dmaven.repo.local=/opt/maven-repositorio -DskipTests package dependency:copy-dependencies -DincludeScope=test -DoutputDirectory=/opt/mailcore/dependencias
CMD ["mvn", "-B", "-ntp", "-o", "-Dmaven.repo.local=/opt/maven-repositorio", "test"]

FROM ${IMAGEM_JAVA} AS verificacao
RUN apt-get update && apt-get install -y --no-install-recommends python3 ca-certificates openssl unzip zip git && rm -rf /var/lib/apt/lists/*
COPY --from=ferramentas /usr/local/bin/docker /usr/local/bin/docker
COPY --from=ferramentas /usr/share/maven /usr/share/maven
RUN ln -s /usr/share/maven/bin/mvn /usr/local/bin/mvn
COPY --from=testes /opt/maven-repositorio /opt/maven-repositorio
COPY --from=testes /opt/mailcore /opt/mailcore
COPY --from=testes /sdk /sdk
WORKDIR /sdk
ENV MAVEN_OPTS="-Xmx768m" MAVEN_CONFIG=/tmp/maven
CMD ["java", "-cp", "target/classes:target/test-classes:/opt/mailcore/dependencias/*", "org.junit.runner.JUnitCore", "br.com.mailcore.sdk.RecursosContratoTest", "br.com.mailcore.sdk.ProtocoloTest", "br.com.mailcore.sdk.AssinaturaTest", "br.com.mailcore.sdk.ModelosTest"]
