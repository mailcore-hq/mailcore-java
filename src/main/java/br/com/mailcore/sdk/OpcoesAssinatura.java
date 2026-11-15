package br.com.mailcore.sdk;

import java.time.Duration;
import java.time.Instant;

/** Janela máxima de cinco minutos; o instante injetado permite testes determinísticos. */
public final class OpcoesAssinatura {
    private final Instant agora;
    private final Duration tolerancia;
    private OpcoesAssinatura(Construtor c) { agora=c.agora; tolerancia=c.tolerancia; }
    public static Construtor construtor() { return new Construtor(); }
    public Instant agora() { return agora; }
    public Duration tolerancia() { return tolerancia; }
    public static final class Construtor {
        private Instant agora;
        private Duration tolerancia=Duration.ofSeconds(300);
        private Construtor() {}
        public Construtor agora(Instant valor) { agora=valor; return this; }
        public Construtor tolerancia(Duration valor) { tolerancia=valor; return this; }
        public OpcoesAssinatura construir() { return new OpcoesAssinatura(this); }
    }
}
