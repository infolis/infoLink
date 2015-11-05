package io.github.infolis.scheduler;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author domi
 */
public class SocketServer  {

    private int port;
    private ServerSocket server = null;
    private Socket socket;

//    public SocketServer(int port) {
//        try {
//            serverSocketChannel = ServerSocketChannel.open();
//            serverSocketChannel.socket().bind(new InetSocketAddress(port));
//        } catch (Exception e) {
//
//        }
//    }

    public SocketServer(int port) {
        this.port = port;
        try {
            server = new ServerSocket(port);
            
            //Socket clientSocket = serverSocket.accept();
        } catch (Exception e) {
        }
    }
    public void write(String message) throws IOException {
        socket = server.accept();
        System.out.println("mess: " + message);
        PrintStream out = new PrintStream(socket.getOutputStream());
        out.print(message);
    }

    public void shutDown() {
        try {
            socket.close();
        } catch (Exception e) {

        }
    }

//    @Override
//    public void run() {
//        while (true) {
//            try {
//                SocketChannel socketChannel = serverSocketChannel.accept();
//            } catch (IOException ex) {
//                Logger.getLogger(SocketServer.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
//    }
}
