package br.com.mailcore.sdk.interno;

import br.com.mailcore.sdk.ErroMailcore;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** Implementação compartilhada do protocolo JSON; não modifica mapeadores da aplicação. */
public final class Json {
    private static final int LIMITE_JSON = 50 * 1024 * 1024;
    private static final JsonFactory FABRICA = JsonFactory.builder()
            .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(100).maxStringLength(LIMITE_JSON).maxNumberLength(10000).build())
            .streamWriteConstraints(StreamWriteConstraints.builder().maxNestingDepth(100).build())
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();
    private static final ObjectMapper MAPEADOR = JsonMapper.builder(FABRICA)
            .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .disable(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();
    private Json() {}

    public static byte[] objeto(Object valor) { return codificar(valor, (byte) '{'); }
    public static byte[] lista(Object valor) { return codificar(valor, (byte) '['); }
    private static byte[] codificar(Object valor, byte inicio) {
        SaidaLimitada saida = new SaidaLimitada();
        try (JsonGenerator gerador = new GeradorSeguro(FABRICA.createGenerator(saida))) {
            MAPEADOR.writeValue(gerador, valor);
        } catch (IOException | RuntimeException erro) {
            throw ErroMailcore.configuracao("Use valores JSON válidos, números finitos e texto Unicode íntegro.");
        }
        byte[] bytes = saida.toByteArray();
        if (bytes.length == 0 || bytes[0] != inicio) { throw ErroMailcore.configuracao("A raiz do pedido não tem a forma JSON exigida."); }
        validar(bytes);
        return bytes;
    }
    public static JsonNode ler(byte[] dados) {
        validar(dados);
        try { return MAPEADOR.readTree(dados); }
        catch (IOException | RuntimeException erro) { throw invalido(); }
    }
    public static <T> T ler(byte[] dados, Class<T> tipo) {
        validar(dados);
        try { return MAPEADOR.readValue(dados, tipo); }
        catch (IOException | RuntimeException erro) { throw invalido(); }
    }
    public static <T> T ler(byte[] dados, TypeReference<T> tipo) {
        validar(dados);
        try { return MAPEADOR.readValue(dados, tipo); }
        catch (IOException | RuntimeException erro) { throw invalido(); }
    }
    public static void validar(byte[] dados) {
        validarUtf8(dados);
        try (JsonParser parser = FABRICA.createParser(dados)) {
            JsonToken token = parser.nextToken();
            if (token == null) { throw invalido(); }
            parser.skipChildren();
            if (parser.nextToken() != null) { throw invalido(); }
        } catch (IOException | RuntimeException erro) { throw invalido(); }
    }
    public static void validarUtf8(byte[] dados) {
        CharsetDecoder decodificador = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer entrada = ByteBuffer.wrap(dados);
        CharBuffer saida = CharBuffer.allocate(2048);
        try {
            while (true) {
                CoderResult resultado = decodificador.decode(entrada, saida, true);
                if (resultado.isError()) { resultado.throwException(); }
                if (resultado.isUnderflow()) { break; }
                saida.clear();
            }
        } catch (CharacterCodingException erro) { throw invalido(); }
    }
    public static void validarTexto(String valor) {
        if (valor == null) { throw ErroMailcore.configuracao("Informe texto válido."); }
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i >= valor.length() || !Character.isLowSurrogate(valor.charAt(i))) { throw ErroMailcore.configuracao("Texto Unicode inválido."); }
            } else if (Character.isLowSurrogate(c)) { throw ErroMailcore.configuracao("Texto Unicode inválido."); }
        }
    }
    public static String segmento(String valor) {
        validarTexto(valor);
        if (valor.isEmpty() || valor.equals(".") || valor.equals("..")) { throw ErroMailcore.configuracao("Informe um identificador não vazio."); }
        for (int i = 0; i < valor.length(); i++) { if (valor.charAt(i) < 32 || valor.charAt(i) == 127) { throw ErroMailcore.configuracao("Identificadores não podem conter controles."); } }
        return escapar(valor);
    }
    public static String escapar(String valor) {
        validarTexto(valor);
        StringBuilder resultado = new StringBuilder();
        char[] hex = "0123456789ABCDEF".toCharArray();
        for (byte b : valor.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 255;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || "-._~".indexOf(c) >= 0) { resultado.append((char) c); }
            else { resultado.append('%').append(hex[c >> 4]).append(hex[c & 15]); }
        }
        return resultado.toString();
    }
    private static ErroMailcore invalido() { return new ErroMailcore("resposta_invalida", "O conteúdo JSON ou UTF-8 é inválido.", null, null); }
    private static final class SaidaLimitada extends ByteArrayOutputStream {
        @Override public synchronized void write(int valor) { if (count >= LIMITE_JSON) { throw ErroMailcore.configuracao("O corpo ultrapassa 50 MiB."); } super.write(valor); }
        @Override public synchronized void write(byte[] bytes, int inicio, int tamanho) { if (tamanho > LIMITE_JSON - count) { throw ErroMailcore.configuracao("O corpo ultrapassa 50 MiB."); } super.write(bytes, inicio, tamanho); }
    }
    private static final class GeradorSeguro extends JsonGeneratorDelegate {
        GeradorSeguro(JsonGenerator original) { super(original, false); }
        @Override public void writeNumber(double valor) throws IOException { if (!Double.isFinite(valor)) { throw ErroMailcore.configuracao("Números JSON devem ser finitos."); } super.writeNumber(valor); }
        @Override public void writeNumber(float valor) throws IOException { if (!Float.isFinite(valor)) { throw ErroMailcore.configuracao("Números JSON devem ser finitos."); } super.writeNumber(valor); }
        @Override public void writeString(String valor) throws IOException { if (valor != null) { validarTexto(valor); } super.writeString(valor); }
        @Override public void writeString(char[] valor, int inicio, int tamanho) throws IOException { validarTexto(new String(valor,inicio,tamanho)); super.writeString(valor,inicio,tamanho); }
        @Override public void writeString(com.fasterxml.jackson.core.SerializableString valor) throws IOException { validarTexto(valor.getValue()); super.writeString(valor); }
        @Override public void writeFieldName(String nome) throws IOException { validarTexto(nome); super.writeFieldName(nome); }
        @Override public void writeFieldName(com.fasterxml.jackson.core.SerializableString nome) throws IOException { validarTexto(nome.getValue()); super.writeFieldName(nome); }
        @Override public void writeArray(double[] valores, int inicio, int tamanho) throws IOException { writeStartArray(valores, tamanho); for (int i=inicio; i<inicio+tamanho; i++) { writeNumber(valores[i]); } writeEndArray(); }
        @Override public void writeArray(String[] valores, int inicio, int tamanho) throws IOException { writeStartArray(valores, tamanho); for (int i=inicio; i<inicio+tamanho; i++) { writeString(valores[i]); } writeEndArray(); }
        @Override public void writeObject(Object valor) throws IOException { MAPEADOR.writeValue(this, valor); }
    }
}
