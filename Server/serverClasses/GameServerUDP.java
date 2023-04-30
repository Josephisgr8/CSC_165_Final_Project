package serverClasses;

import tage.*;
import org.joml.*;
import java.util.*;
import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;

public class GameServerUDP extends GameConnectionServer<UUID>
{
    public GameServerUDP(int localPort) throws IOException{ 
        super(localPort, ProtocolType.UDP);
    }

    @Override
    public void processPacket(Object o, InetAddress senderIP, int sndPort){
        String message = (String) o;
        String[] msgTokens = message.split(",");
        if(msgTokens.length > 0) {
            // case where server receives a JOIN message
            // format: join,localid
            if(msgTokens[0].compareTo("join") == 0){
                System.out.println("recieved join msg"); 
                try{ 
                    IClientInfo ci;
                    ci = getServerSocket().createClientInfo(senderIP, sndPort);
                    UUID clientID = UUID.fromString(msgTokens[1]);
                    addClient(ci, clientID);
                    sendJoinedMessage(clientID, true);
                    System.out.println("success for join msg from " + clientID.toString());
                }
                catch (IOException e){
                    e.printStackTrace();
                } 
            }
            // case where server receives a CREATE message
            // format: create,localid,x,y,z
            if(msgTokens[0].compareTo("create") == 0){
                UUID clientID = UUID.fromString(msgTokens[1]);
                String[] pos = {msgTokens[2], msgTokens[3], msgTokens[4]};
                System.out.println("recieved create msg from " + clientID.toString());
                sendCreateMessages(clientID, pos);
                sendWantsDetailsMessages(clientID);
            }
            // case where server receives a BYE message
            // format: bye,localid
            if(msgTokens[0].compareTo("bye") == 0){
                UUID clientID = UUID.fromString(msgTokens[1]);
                System.out.println("recieved bye msg from " + clientID.toString());
                sendByeMessages(clientID);
                removeClient(clientID);
            }
            // case where server receives a DETAILS-FOR message
            if(msgTokens[0].compareTo("dsfr") == 0){
                System.out.println("receieved dsfr msg");
                UUID newPlayerID = UUID.fromString(msgTokens[1]);
                System.out.println("recieved newPlayerID: " + newPlayerID.toString());
                UUID sendingPlayerID = UUID.fromString(msgTokens[2]);
                System.out.println("recieved sending player's id: " + sendingPlayerID.toString());
                String[] sendingPlayerPos = {msgTokens[3], msgTokens[4], msgTokens[5]};
                System.out.println("recieved sendingplayer coords: " + sendingPlayerPos);
                
                sendDetailsMsg(newPlayerID, sendingPlayerID, sendingPlayerPos);
                // etc..... 
            }
            // case where server receives a MOVE message
            if(msgTokens[0].compareTo("move") == 0) {
                // etc..... }
                UUID clientID = UUID.fromString(msgTokens[1]);
                String[] newPlayerPos = {msgTokens[2], msgTokens[3], msgTokens[4]};
                sendMoveMessages(clientID, newPlayerPos);
            }
        }
    }

    public void sendJoinedMessage(UUID clientID, boolean success){
        // format: join, success or join, failure
        try{
            String message = new String("join,");
            if (success) message += "success";
            else message += "failure";
            sendPacket(message, clientID);
            System.out.println("sending join msg to new client " + clientID.toString());
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    public void sendCreateMessages(UUID clientID, String[] position){
        // format: create, remoteId, x, y, z
        try{
            String message = new String("create," + clientID.toString());
            message += "," + position[0];
            message += "," + position[1];
            message += "," + position[2];
            forwardPacketToAll(message, clientID);
            System.out.println("sent create msg to all clients " + clientID.toString());
        }
        catch (IOException e) { e.printStackTrace();}
    }

    public void sendDetailsMsg(UUID clientID, UUID remoteId, String[] position){
        // etc..... 
        try {
            String message = new String("dsfr," + remoteId.toString());
            message += "," + position[0];
            message += "," + position[1];
            message += "," + position[2];
            System.out.println("about to send dsfr msg to new client");
            sendPacket(message, clientID);
            System.out.println("sent dsrf msg to client: " + clientID.toString());
        }
        catch (IOException e) {e.printStackTrace();}
    }

    public void sendWantsDetailsMessages(UUID clientID){
        // etc.....  This clientID wants EVERYONES details
        try {
            String message = new String("wsds," + clientID.toString());
            forwardPacketToAll(message, clientID);
            System.out.println("sent wsds msg to all clients for " + clientID.toString());
        }
        catch (IOException e) {e.printStackTrace();}
    }

    public void sendMoveMessages(UUID clientID, String[] position){
        // etc.....  Tell everyone that this client just moved
        try {
            String message = new String("move," + clientID.toString());
            message += "," + position[0];
            message += "," + position[1];
            message += "," + position[2];
            forwardPacketToAll(message, clientID);
        }
        catch (IOException e) {e.printStackTrace();}
    }

    public void sendByeMessages(UUID clientID){
        // etc..... }
        try {
            String message = new String("bye," + clientID.toString());
            System.out.println("sending bye msg to all clients from " + clientID.toString());
            forwardPacketToAll(message, clientID);
        }
        catch (IOException e) {e.printStackTrace();}
    }

}

