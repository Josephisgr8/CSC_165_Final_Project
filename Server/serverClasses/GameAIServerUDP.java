package serverClasses;

import tage.*;
import org.joml.*;
import java.util.*;
import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import tage.networking.server.GameConnectionServer;
import tage.networking.server.IClientInfo;

public class GameAIServerUDP extends GameConnectionServer<UUID>{

    NPCController npcCtrl;

    public GameAIServerUDP(int localPort, NPCController npc) throws IOException{
        super(localPort, ProtocolType.UDP);
        npcCtrl = npc;
    }

    @Override
    public void processPacket(Object o, InetAddress senderIP, int port){
        String message = (String) o;
        String[] msgTokens = message.split(",");
        if(msgTokens.length > 0) {
            // case where server receives a JOIN message
            // format: join,localid
            if(msgTokens[0].compareTo("join") == 0){
                System.out.println("recieved join msg"); 
                try{ 
                    IClientInfo ci;
                    ci = getServerSocket().createClientInfo(senderIP, port);
                    UUID clientID = UUID.fromString(msgTokens[1]);
                    addClient(ci, clientID);
                    sendJoinedMessage(clientID, true);
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
                System.out.println("recieved create msg from ID: " + clientID.toString());
                sendCreateMessages(clientID, pos);
                sendWantsDetailsMessages(clientID);
                sendCreateNPCmsg(clientID);
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

            //NPC STUFF

            // Case where server receives request for NPCs
            // Received Message Format: (needNPC,id)
            if(msgTokens[0].compareTo("needNPC") == 0){
                System.out.println("server got a needNPC message");
                UUID clientID = UUID.fromString(msgTokens[1]);
                sendNPCstart(clientID);
            }
            // Case where server receives notice that an av is close to the npc
            // Received Message Format: (isnear,id)
            if(msgTokens[0].compareTo("isnear") == 0){
                UUID clientID = UUID.fromString(msgTokens[1]);
                handleNearTiming(clientID);
            } 
        }    
    }

    public void handleNearTiming(UUID clientID){
        npcCtrl.setNearFlag(true);
    }

    //------------- SENDING PLAYER MESSAGES --------------
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

    // ------------ SENDING NPC MESSAGES -----------------
    // Informs clients of the whereabouts of the NPCs.
    public void sendCreateNPCmsg(UUID clientID){
        try{
            System.out.println("server telling clients about an NPC");
            String message = new String("createNPC,");
            message += "," + "20";
            message += "," + "1";
            message += "," + "20";
            forwardPacketToAll(message, clientID);
        } 
        catch (IOException e) { e.printStackTrace(); }
    } 

    // --- additional protocol for NPCs ----
    public void sendCheckForAvatarNear(){
        try{
            String message = new String("isnr");
            message += "," + (npcCtrl.getNPC()).getX();
            message += "," + (npcCtrl.getNPC()).getY();
            message += "," + (npcCtrl.getNPC()).getZ();
            message += "," + (npcCtrl.getCriteria());
            sendPacketToAll(message);
        }
        catch (IOException e){ System.out.println("couldnt send msg"); e.printStackTrace(); }
        }

    public void sendNPCinfo(){
        //code here
        try{
            String message = new String("npcinfo");
            message += "," + (npcCtrl.getNPC().getX());
            message += "," + (npcCtrl.getNPC().getY());
            message += "," + (npcCtrl.getNPC().getZ());
            message += "," + (npcCtrl.getNPC().getSize());
            sendPacketToAll(message);
        }
        catch (IOException e){System.out.println("couldn't send NPC info"); e.printStackTrace();}
    }

    public void sendNPCstart(UUID clientID){
        //code here
    }
}

