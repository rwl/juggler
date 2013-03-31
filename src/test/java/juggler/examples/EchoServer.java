package juggler.examples;

import static juggler.Juggler.go;

import juggler.Juggler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer {

    static final int PORT = 2020;

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            System.err.printf("Listen error on port: %d.", PORT);
            System.exit(1);
        }

        int accepted = 0;
        while (true) {
            Socket clientSocket = null;
            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.err.println("Accept error.");
                System.exit(1);
            }
            go(new Server(), clientSocket);
            accepted += 1;
        }
    }

    static class Server implements Juggler.Consumer<Socket> {
        @Override
        public void run(Socket socket)  {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                out.println(in.readLine());

                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                System.err.println("I/O error");
                System.exit(1);
            }
        }
    }
}
