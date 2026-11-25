package Client;
import Model.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class Client {
    // Connection variables
    private final String name;
    private final int port;
    private Socket client;

    // Streams
    private ObjectInputStream input;
    private ObjectOutputStream output;

    public Client(String name, int port) {
        this.name = name;
        this.port = port;
        this.client = null;
    }

    public void connectClient() throws IOException {
        // TCP connection
        this.client = new Socket(name, port);
        System.err.println("Connected\n");

        // Streams
        this.input = new ObjectInputStream(client.getInputStream());
        this.output = new ObjectOutputStream(client.getOutputStream());
    }

    public void close() throws Exception {
        // Close
        client.close();
        output.close();
        input.close();
    }

    public ObjectInputStream getInput() {
        return input;
    }

    public ObjectOutputStream getOutput() {
        return output;
    }

    public static void main(String[] args) {
        Client client = new Client("localhost", 6060);
        Scanner sc = new Scanner(System.in);
        try {
            client.connectClient();

            Thread writing = new Thread() {
                @Override 
                public void run() {
                    while (true) {
                        // Protocol send
                        String in = sc.nextLine();

                        if (in.equals("0")) {
                            try {
                                RoomData roomData = new RoomData();
                                roomData.setRoomName("Saccu");
                                roomData.setPin("12345");
                                roomData.setMinPlayers(2);
                                roomData.setMaxPlayers(10);
                                roomData.setnLives(3);
                                roomData.setActionTimer(10);
                                roomData.setTimeoutKick(60);
                                
                                client.getOutput().writeObject("R01");
                                client.getOutput().writeObject(roomData);
                            } catch (IOException e) {
                                System.err.println("Error in stream output: " + e.getMessage());
                            }
                        }
                        else if (in.equals("1")) {
                            try {
                                ArrayList<String> namePin = new ArrayList<>();
                                namePin.add("Saccu");
                                namePin.add("12345");
                                namePin.add("Pierpi133");
                                
                                client.getOutput().writeObject("R02");
                                client.getOutput().writeObject(namePin);
                            } catch (IOException e) {
                                System.err.println("Error in stream output: " + e.getMessage());
                            }
                        }
                        else {
                            try {
                                client.getOutput().writeObject(in);
                            } catch (IOException e) {
                                System.err.println("Error in stream output: " + e.getMessage());
                            }
                        }
                    }
                }
            };
            writing.start();
            

            while (true) {
                try {
                    // Protocol response
                    Object message = client.getInput().readObject();

                    if (message instanceof String msg) {
                        if (msg.equals("0")) {
                            System.out.println("Server closed the connection");
                            break;
                        }

                        if (msg.equals("R011")) {
                            System.out.println("Joined the room you created with success");
                        }
                        if (msg.equals("R010")) {
                            System.out.println("Error creating the room");
                        }
                        if (msg.equals("R021")) {
                            System.out.println("Joined the room with success");
                        }
                        if (msg.equals("R020")) {
                            System.out.println("Error joining the room");
                        }

                        if (msg.equals("S1")) {
                            System.out.println("Game started");    
                        }
                        if (msg.equals("S0")) {
                            System.out.println("Failed to start the game");    
                        }

                        if (msg.equals("PD")) {
                            Object data = (GameData)client.getInput().readObject();
                            System.out.println(data.toString());
                        }
                        if (msg.equals("P")) {
                            System.out.println("INSERISCI SCELTA: ");
                        }

                        if (msg.equals("V")) {
                            Object data = (String)client.getInput().readObject();
                            System.out.println("VINCITORE: " + data);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error in stream input: " + e.getMessage());
                }                
            }
        } catch (Exception e) {
            System.err.println("Error while connecting to server: " + e.getMessage());
        }
        finally {
            try {
                sc.close();
                client.close();
            } catch (Exception e) {
                System.err.println("Error while closing client: " + e.getMessage());
            }
        }
    }
}
