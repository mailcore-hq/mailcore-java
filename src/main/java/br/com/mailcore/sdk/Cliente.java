package br.com.mailcore.sdk;

import br.com.mailcore.sdk.interno.Transporte;
import br.com.mailcore.sdk.recursos.Emails;
import br.com.mailcore.sdk.recursos.Lotes;
import br.com.mailcore.sdk.recursos.Dominios;
import br.com.mailcore.sdk.recursos.ChavesApi;
import br.com.mailcore.sdk.recursos.Contatos;
import br.com.mailcore.sdk.recursos.Segmentos;
import br.com.mailcore.sdk.recursos.Topicos;
import br.com.mailcore.sdk.recursos.PropriedadesContatos;
import br.com.mailcore.sdk.recursos.Campanhas;
import br.com.mailcore.sdk.recursos.Modelos;
import br.com.mailcore.sdk.recursos.Automacoes;
import br.com.mailcore.sdk.recursos.Webhooks;
import br.com.mailcore.sdk.recursos.Recebimento;
import br.com.mailcore.sdk.recursos.Caixas;
import br.com.mailcore.sdk.recursos.Eventos;
import br.com.mailcore.sdk.recursos.CliMailcore;

/** Cliente síncrono compartilhável entre threads; feche apenas ao encerrar a aplicação. */
public final class Cliente implements AutoCloseable {
    private final Transporte transporte;
    private final Emails emails;
    private final Lotes lotes;
    private final Dominios dominios;
    private final ChavesApi chavesApi;
    private final Contatos contatos;
    private final Segmentos segmentos;
    private final Topicos topicos;
    private final PropriedadesContatos propriedadesContatos;
    private final Campanhas campanhas;
    private final Modelos modelos;
    private final Automacoes automacoes;
    private final Webhooks webhooks;
    private final Recebimento recebimento;
    private final Caixas caixas;
    private final Eventos eventos;
    private final CliMailcore cli;

    /** Resolve MAILCORE_API_KEY a cada operação. */
    public Cliente() { this(null, Configuracao.padrao()); }
    /** Usa uma chave explícita. */
    public Cliente(String chave) { this(chave, Configuracao.padrao()); }
    /** Usa configuração imutável e chave explícita, variável de ambiente ou fornecedor. */
    public Cliente(String chave, Configuracao configuracao) {
        transporte = new Transporte(chave, configuracao);
        emails = new Emails(transporte);
        lotes = new Lotes(transporte);
        dominios = new Dominios(transporte);
        chavesApi = new ChavesApi(transporte);
        contatos = new Contatos(transporte);
        segmentos = new Segmentos(transporte);
        topicos = new Topicos(transporte);
        propriedadesContatos = new PropriedadesContatos(transporte);
        campanhas = new Campanhas(transporte);
        modelos = new Modelos(transporte);
        automacoes = new Automacoes(transporte);
        webhooks = new Webhooks(transporte);
        recebimento = new Recebimento(transporte);
        caixas = new Caixas(transporte);
        eventos = new Eventos(transporte);
        cli = new CliMailcore(transporte);
    }

    /** Retorna as operações de emails. */
    public Emails emails() { return emails; }
    /** Retorna as operações de lotes. */
    public Lotes lotes() { return lotes; }
    /** Retorna as operações de dominios. */
    public Dominios dominios() { return dominios; }
    /** Retorna as operações de chavesApi. */
    public ChavesApi chavesApi() { return chavesApi; }
    /** Retorna as operações de contatos. */
    public Contatos contatos() { return contatos; }
    /** Retorna as operações de segmentos. */
    public Segmentos segmentos() { return segmentos; }
    /** Retorna as operações de topicos. */
    public Topicos topicos() { return topicos; }
    /** Retorna as operações de propriedadesContatos. */
    public PropriedadesContatos propriedadesContatos() { return propriedadesContatos; }
    /** Retorna as operações de campanhas. */
    public Campanhas campanhas() { return campanhas; }
    /** Retorna as operações de modelos. */
    public Modelos modelos() { return modelos; }
    /** Retorna as operações de automacoes. */
    public Automacoes automacoes() { return automacoes; }
    /** Retorna as operações de webhooks. */
    public Webhooks webhooks() { return webhooks; }
    /** Retorna as operações de recebimento. */
    public Recebimento recebimento() { return recebimento; }
    /** Retorna as operações de caixas. */
    public Caixas caixas() { return caixas; }
    /** Retorna as operações de eventos. */
    public Eventos eventos() { return eventos; }
    /** Retorna as operações de cli. */
    public CliMailcore cli() { return cli; }

    /** Cancela operações em curso e impede novas chamadas deste cliente. */
    public void fechar() { transporte.close(); }
    @Override public void close() { fechar(); }
    @Override public String toString() { return "Cliente{credenciais=protegidas}"; }
}
