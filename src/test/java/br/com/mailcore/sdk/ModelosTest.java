package br.com.mailcore.sdk;

import static org.junit.Assert.*;

import br.com.mailcore.sdk.interno.Json;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** Confere objetos públicos, cópias defensivas e a fronteira de serialização. */
public class ModelosTest {
    @Test public void pedidoDiretoUsaContratoPortuguesESnapshotImutavel() {
        List<String> destinatarios=new ArrayList<>(Arrays.asList("pessoa@example.test"));
        Map<String,String> tags=new LinkedHashMap<>(); tags.put("origem","teste");
        PedidoEmail.Construtor construtor=PedidoEmail.construtor().remetente("origem@example.test")
                .destinatarios(destinatarios).assunto("Olá 📬").html("<b>Olá</b>").texto("Olá")
                .responderPara("suporte@example.test").agendadaEm("2026-11-15T12:00:00Z")
                .tags(tags).cc(Collections.singletonList("copia@example.test"))
                .cco(Collections.singletonList("oculta@example.test"))
                .cabecalhos(Collections.singletonMap("X-Origem","sintetica"))
                .anexos(Collections.singletonList(AnexoEmail.construtor().nome("imagem.png")
                        .tipoConteudo("image/png").conteudoBase64("AAE=").idConteudo("imagem").construir()));
        PedidoEmail pedido=construtor.construir();
        destinatarios.clear(); tags.clear(); construtor.assunto("alterado");
        JsonNode dados=Json.ler(Json.objeto(pedido));
        assertEquals("Olá 📬",dados.get("assunto").asText());
        assertEquals("pessoa@example.test",dados.at("/destinatarios/0").asText());
        assertEquals("suporte@example.test",dados.get("responder_para").asText());
        assertEquals("teste",dados.at("/tags/origem").asText());
        assertEquals("imagem",dados.at("/anexos/0/id_conteudo").asText());
        assertEquals("image/png",dados.at("/anexos/0/tipo_conteudo").asText());
        ((com.fasterxml.jackson.databind.node.ObjectNode)pedido.dados()).removeAll();
        assertEquals(dados,pedido.dados());
        assertFalse(pedido.toString().contains("example.test"));
        assertFalse(construtor.toString().contains("alterado"));
    }

    @Test public void modeloPreservaNumeroNuloEObjetoVazio() {
        Map<String,Object> variaveis=new LinkedHashMap<>();
        variaveis.put("nome","Pessoa"); variaveis.put("valor",new java.math.BigDecimal("10.00"));
        ModeloEmail modelo=ModeloEmail.construtor().id("modelo-sintetico").variaveis(variaveis).construir();
        PedidoEmail pedido=PedidoEmail.construtor().destinatarios(Collections.singletonList("destino@example.test"))
                .remetente(null).modelo(modelo).construir();
        variaveis.clear();
        assertEquals("10.00",pedido.dados().at("/modelo/variaveis/valor").asText());
        assertTrue(pedido.dados().has("remetente"));
        assertTrue(pedido.dados().get("remetente").isNull());
        assertFalse(pedido.dados().has("html"));
        assertFalse(modelo.toString().contains("Pessoa"));
        assertTrue(ModeloEmail.construtor().id("modelo").variaveis(Collections.emptyMap()).construir().dados().get("variaveis").isObject());
    }

    @Test public void respostaTipadaNaoConfundeAceiteEEntrega() {
        Resposta resposta=new Resposta("{\"id\":\"sintetico\",\"estado\":\"aceita\",\"futuro\":true}".getBytes(StandardCharsets.UTF_8),202,null,1);
        RespostaEnvio envio=resposta.decodificar(RespostaEnvio.class);
        assertEquals("sintetico",envio.id()); assertEquals("aceita",envio.estado());
        assertFalse(envio.toString().contains("sintetico"));
        assertFalse(resposta.toString().contains("aceita"));
    }

    @Test public void unicodeInvalidoRecusadoEmTodosOsAtalhosDoGerador() {
        for(Object valor:Arrays.<Object>asList("\uD800",new char[]{'\uD800'},new String[]{"\uD800"},
                new String[]{"\uDC00"},new double[]{Double.NaN},Double.POSITIVE_INFINITY)) {
            try { Json.objeto(Collections.singletonMap("campo",valor)); fail("Texto/número inválido foi aceito."); }
            catch(ErroMailcore esperado) { assertNull(esperado.getCause()); }
        }
    }

    @Test public void configuracaoProtegeMaterialERejeitaAmbiguidade() {
        byte[] ca={1,2,3}; Configuracao c=Configuracao.construtor().certificadoCa(ca).construir();
        ca[0]=9; assertEquals(1,c.certificadoCa()[0]);
        byte[] copia=c.certificadoCa(); copia[0]=8; assertEquals(1,c.certificadoCa()[0]);
        for(Configuracao invalida:Arrays.asList(Configuracao.construtor().urlBase("http://externo.example.test/v1").permitirHttpLocal(true).construir(),
                Configuracao.construtor().urlBase("https://usuario:senha@example.test/v1").construir(),
                Configuracao.construtor().urlBase("https://example.test/v1?segredo=1").construir(),
                Configuracao.construtor().tempoLimite(Duration.ZERO).construir(),
                Configuracao.construtor().tempoConexao(Duration.ofDays(2)).construir(),
                Configuracao.construtor().limiteRespostaBytes(0).construir(),
                Configuracao.construtor().fornecedorChave(()->"outra").construir())) {
            try { new Cliente("chave",invalida).close(); fail("Configuração inválida foi aceita."); }
            catch(ErroMailcore esperado) { assertEquals("configuracao_invalida",esperado.codigo()); assertNull(esperado.getCause()); }
        }
    }
}
