package de.lgohlke.signal;

import org.whispersystems.signalservice.api.websocket.ConnectivityListener;

import static org.asamk.signal.util.LogUtils.debug;

class DefaultConnectivityListener implements ConnectivityListener {

    @Override
    public void onConnected() {
        debug("connected");
    }

    @Override
    public void onConnecting() {
        debug("connecting");
    }

    @Override
    public void onDisconnected() {
        debug("disconnected");
    }

    @Override
    public void onAuthenticationFailure() {
        debug("auth failure");
    }
}
