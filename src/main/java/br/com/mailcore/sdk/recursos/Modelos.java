// Gerado por scripts/gerar_recursos.py a partir das rotas públicas Mailcore.
package br.com.mailcore.sdk.recursos;

import br.com.mailcore.sdk.ErroMailcore;
import br.com.mailcore.sdk.OpcoesRequisicao;
import br.com.mailcore.sdk.Resposta;
import br.com.mailcore.sdk.interno.Formato;
import br.com.mailcore.sdk.interno.Json;
import br.com.mailcore.sdk.interno.Transporte;
import java.util.Collections;
import java.util.Map;

/** Operações do recurso Modelos no contrato público Mailcore. */
public final class Modelos {
    private final Transporte transporte;
    private final VersoesModelo versoes;
    private final PastasModelo pastas;

    /** Compartilha o transporte configurado pelo cliente. */
    public Modelos(Transporte transporte) {
        if (transporte == null) {
            throw ErroMailcore.configuracao("Informe o transporte Mailcore.");
        }
        this.transporte = transporte;
        this.versoes = new VersoesModelo(transporte);
        this.pastas = new PastasModelo(transporte);
    }

    /** Retorna as operações de versoes. */
    public VersoesModelo versoes() {
        return versoes;
    }

    /** Retorna as operações de pastas. */
    public PastasModelo pastas() {
        return pastas;
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta criar(Object pedido) {
        return criar(pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta criar(Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/templates",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta listar(Map<String, ?> filtros) {
        return listar(filtros, null);
    }

    /** Usa os parâmetros opcionais padrão da operação. */
    public Resposta listar() {
        return listar(Collections.<String, Object>emptyMap());
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta listar(Map<String, ?> filtros, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/templates",
                null, filtros, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta obter(String id) {
        return obter(id, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta obter(String id, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/templates/" + Json.segmento(id),
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta atualizar(String id, Object pedido) {
        return atualizar(id, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta atualizar(String id, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "PATCH", "/templates/" + Json.segmento(id),
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta remover(String id) {
        return remover(id, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta remover(String id, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "DELETE", "/templates/" + Json.segmento(id),
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta prever(String id, Object pedido) {
        return prever(id, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta prever(String id, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/templates/" + Json.segmento(id) + "/preview",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta obterPrevia(String organizacaoId, String previaId) {
        return obterPrevia(organizacaoId, previaId, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta obterPrevia(String organizacaoId, String previaId, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/template-previews/" + Json.segmento(organizacaoId) + "/" + Json.segmento(previaId),
                null, null, Formato.TEXTO, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta testar(String id, Object pedido) {
        return testar(id, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta testar(String id, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/templates/" + Json.segmento(id) + "/test",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta publicar(String id, Object pedido) {
        return publicar(id, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta publicar(String id, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/templates/" + Json.segmento(id) + "/publish",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta duplicar(String id, Object pedido) {
        return duplicar(id, pedido, null);
    }

    /** Usa os parâmetros opcionais padrão da operação. */
    public Resposta duplicar(String id) {
        return duplicar(id, Collections.<String, Object>emptyMap());
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta duplicar(String id, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/templates/" + Json.segmento(id) + "/duplicate",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }
}
