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

/** Operações do recurso Contatos no contrato público Mailcore. */
public final class Contatos {
    private final Transporte transporte;
    private final SegmentosContato segmentos;
    private final TopicosContato topicos;
    private final ImportacoesContatos importacoes;
    private final ExportacoesContatos exportacoes;

    /** Compartilha o transporte configurado pelo cliente. */
    public Contatos(Transporte transporte) {
        if (transporte == null) {
            throw ErroMailcore.configuracao("Informe o transporte Mailcore.");
        }
        this.transporte = transporte;
        this.segmentos = new SegmentosContato(transporte);
        this.topicos = new TopicosContato(transporte);
        this.importacoes = new ImportacoesContatos(transporte);
        this.exportacoes = new ExportacoesContatos(transporte);
    }

    /** Retorna as operações de segmentos. */
    public SegmentosContato segmentos() {
        return segmentos;
    }

    /** Retorna as operações de topicos. */
    public TopicosContato topicos() {
        return topicos;
    }

    /** Retorna as operações de importacoes. */
    public ImportacoesContatos importacoes() {
        return importacoes;
    }

    /** Retorna as operações de exportacoes. */
    public ExportacoesContatos exportacoes() {
        return exportacoes;
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta criar(Object pedido) {
        return criar(pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta criar(Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/contacts",
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
                "GET", "/contacts",
                null, filtros, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta obter(String id) {
        return obter(id, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta obter(String id, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "GET", "/contacts/" + Json.segmento(id),
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta atualizar(String id, Object pedido) {
        return atualizar(id, pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta atualizar(String id, Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "PATCH", "/contacts/" + Json.segmento(id),
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta remover(String id) {
        return remover(id, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta remover(String id, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "DELETE", "/contacts/" + Json.segmento(id),
                null, null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta adicionarEmLote(Object pedido) {
        return adicionarEmLote(pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta adicionarEmLote(Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/contacts/batch",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }

    /** Executa a operação correspondente do contrato público Mailcore. */
    public Resposta alterarEmLote(Object pedido) {
        return alterarEmLote(pedido, null);
    }

    /** Executa a operação com as opções desta requisição. */
    public Resposta alterarEmLote(Object pedido, OpcoesRequisicao opcoes) {
        return transporte.executar(
                "POST", "/contacts/bulk",
                Json.objeto(pedido), null, Formato.JSON, false, opcoes, null);
    }
}
