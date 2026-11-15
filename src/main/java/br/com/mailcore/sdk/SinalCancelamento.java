package br.com.mailcore.sdk;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import okhttp3.Call;

/** Cancelamento por operação ou grupo, sem modificar configuração global da JVM. */
public final class SinalCancelamento {
    private boolean cancelado;
    private final Set<Call> chamadas = new HashSet<>();
    public void cancelar() {
        List<Call> pendentes;
        synchronized (this) {
            if (cancelado) { return; }
            cancelado = true;
            pendentes = new ArrayList<>(chamadas);
            chamadas.clear();
        }
        for (Call chamada : pendentes) { chamada.cancel(); }
    }
    public synchronized boolean cancelado() { return cancelado; }
    /** Uso interno do transporte; não é necessário registrar chamadas na aplicação. */
    public synchronized boolean registrar(Call chamada) {
        if (cancelado) { return false; }
        chamadas.add(chamada);
        return true;
    }
    /** Uso interno: remove a inscrição ao concluir a transferência. */
    public synchronized void remover(Call chamada) { chamadas.remove(chamada); }
    @Override public String toString() { return "SinalCancelamento"; }
}
