package br.com.mailcore.sdk;

import br.com.mailcore.sdk.interno.Json;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;

/** Resposta própria e imutável, com bytes originais e metadados separados. */
public final class Resposta {
    private final byte[] dados;
    private final int status;
    private final Map<String,List<String>> cabecalhos;
    private final long totalBytes;
    public Resposta(byte[] dados,int status,Map<String,List<String>> cabecalhos,long totalBytes) { this.dados=dados.clone(); this.status=status; this.cabecalhos=Cabecalhos.copiar(cabecalhos); this.totalBytes=totalBytes; }
    public byte[] dados() { return dados.clone(); }
    public int status() { return status; }
    public Map<String,List<String>> cabecalhos() { return cabecalhos; }
    public long totalBytes() { return totalBytes; }
    public <T> T decodificar(Class<T> tipo) { return Json.ler(dados,tipo); }
    public <T> T decodificar(TypeReference<T> tipo) { return Json.ler(dados,tipo); }
    public JsonNode json() { return Json.ler(dados); }
    @Override public String toString() { return "Resposta{status="+status+",totalBytes="+totalBytes+"}"; }
}
