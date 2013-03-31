package juggler.examples;

import static juggler.Selector.select;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class EchoClient {

    static final String HOST = "localhost";
    static final int N = 20000;

    public static void main(String[] args) throws IOException {
        Socket[] sockets = new Socket[N];

        for (int i = 0; i < N; i++) {
            Socket socket = null;
            PrintWriter out = null;
            BufferedReader in = null;

            try {
                socket = new Socket(HOST, EchoServer.PORT);
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket
                        .getInputStream()));
            } catch (UnknownHostException e) {
                System.err.printf("Unknown host: %s", HOST);
                System.exit(1);
            } catch (IOException e) {
                System.err.printf("Problem connecting to: %s", HOST);
                System.exit(1);
            }

            out.println("sent: " + i);
            System.out.println("echo: " + in.readLine());

            out.close();
            in.close();
            //socket.close();
            sockets[i] = socket;
        }

        select(null);
    }
}
