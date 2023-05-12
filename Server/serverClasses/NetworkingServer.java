package serverClasses;

import java.io.IOException;
import tage.networking.IGameConnection.ProtocolType;

public class NetworkingServer{

    private GameAIServerUDP UDPServer;
    private NPCController npcController;

    public NetworkingServer(int serverPort){
        npcController = new NPCController();

        try {
            UDPServer = new GameAIServerUDP(serverPort, npcController);
        }
        catch (IOException e){
            System.out.println("Server didn't start");
            e.printStackTrace();
        } 

        npcController.start(UDPServer);
    }

    public static void main(String[] args){
        if(args.length == 1){
            NetworkingServer app = new NetworkingServer(Integer.parseInt(args[0]));
        } 
    } 
}