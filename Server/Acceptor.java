package Server;

public class Acceptor extends Thread {
    private final Server handler;

    private boolean running = true;

    public Acceptor(Server handler) {
        this.handler = handler;
    }

    @Override 
    public void interrupt() {
        this.running = false;
        super.interrupt();
    }

    @Override 
    public void run() {
        while (running) {
            try {
                handler.listenClient();
            } catch (Exception e) {
                System.out.println("Error while accepting client: " + e.getMessage());
            }
        }
    }
}
