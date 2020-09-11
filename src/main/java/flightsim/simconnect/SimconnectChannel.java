/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package flightsim.simconnect;

import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Gensiub
 */
public class SimconnectChannel {

    private final SocketChannel sc;
    private final FileChannel fc;

    public SimconnectChannel(SocketChannel sc) {
        this.sc = sc;
        this.fc = null;
    }

    public SimconnectChannel(FileChannel fc) {
        this.sc = null;
        this.fc = fc;
    }

    public Socket socket() {
        if (sc == null) {
            throw new IllegalStateException("socket() called on FileChannel!");
        }
        return sc.socket();
    }

    public void close() throws IOException {
        if (sc == null) {
            fc.close();
        } else {
            sc.close();
        }
    }

    public boolean isConnected() {
        if (sc == null) {
            return fc.isOpen();
        }
        return sc.isConnected();
    }
    
    public int write(ByteBuffer src) throws IOException {
        if(sc == null) {
            return fc.write(src);
        }
        return sc.write(src);
    }
    
    public int read(ByteBuffer dst) throws IOException {
        if(sc == null) {
            return fc.read(dst);
        }
        return sc.read(dst);
    }
}
