package myGame;

import tage.*;
//import tage.rml.*;
import java.util.UUID;
import org.joml.*;
import java.util.*;
import java.io.*;
import java.net.InetAddress;
import tage.networking.client.*;

public class ProtocolClient extends GameConnectionClient{

    private MyGame game;
    private UUID id;
    private GhostManager ghostManager;
    private GhostNPC ghostnpc;
    private float initScale;

    public ProtocolClient(
    InetAddress remAddr, int remPort, ProtocolType pType, MyGame game, float iScale) throws IOException{

        super(remAddr, remPort, pType);
        this.game = game;
        this.id = UUID.randomUUID();
        ghostManager = game.getGhostManager();
        initScale = iScale;
    }

    @Override
    protected void processPacket(Object msg){
        String strMessage = (String)msg;
        String[] messageTokens = strMessage.split(",");

        if(messageTokens.length > 0){
            if(messageTokens[0].compareTo("join") == 0) {
                System.out.println("processing join msg");
                // format: join, success or join, failure
                // receive join
                if(messageTokens[1].compareTo("success") == 0){
                    game.setIsConnected(true);
                    sendCreateMessage(game.getPlayerPosition());
                }
                if(messageTokens[1].compareTo("failure") == 0){
                    game.setIsConnected(false);
                    System.out.println("failed to join");
                } 
            }
        }

        if(messageTokens[0].compareTo("bye") == 0) {
            // format: bye, remoteId
            // receive bye
            System.out.println("recieved bye msg from server");
            UUID ghostID = UUID.fromString(messageTokens[1]);
            if (ghostID == id) {return;}
            ghostManager.removeGhostAvatar(ghostID);
        }
    
        if ((messageTokens[0].compareTo("dsfr") == 0 )
        || (messageTokens[0].compareTo("create")==0)){
            // format: create, remoteId, x,y,z or dsfr, remoteId, x,y,z
            // receive dsfr
            System.out.println("recieved dsfr/create msg");
            UUID ghostID = UUID.fromString(messageTokens[1]);
            System.out.println("created ghostID from remote ID");
            if (ghostID == id) {System.out.println("this is my id"); return;} //This is me
            Vector3f ghostPosition = new Vector3f(
            Float.parseFloat(messageTokens[2]),
            Float.parseFloat(messageTokens[3]),
            Float.parseFloat(messageTokens[4]));
            System.out.println("created vector of remote client's position");
            try{
                ghostManager.createGhost(ghostID, ghostPosition, initScale);
                System.out.println("created ghost for " + ghostID.toString());
                }
            catch (IOException e){
                System.out.println("error creating ghost avatar");
            }
        }
    

        if(messageTokens[0].compareTo("wsds") == 0) {
            UUID remoteID = UUID.fromString(messageTokens[1]);
            if (remoteID == id) {return;} // This is me
            sendDetailsForMessage(remoteID, game.getPlayerPosition());
        }

        if(messageTokens[0].compareTo("move") == 0) {
            // etc..... }
            // rec. move update that ghost
            UUID remoteID = UUID.fromString(messageTokens[1]);
            Vector3f ghostNewPos = new Vector3f(
                Float.parseFloat(messageTokens[2]),
                Float.parseFloat(messageTokens[3]),
                Float.parseFloat(messageTokens[4]));
            try {
                ghostManager.updateGhostAvatar(remoteID, ghostNewPos);
            }
            catch (IOException e) { System.out.println("Error updating ghost avatar position.");}
        } 

        // more additions to the network protocol to handle ghosts:
        if (messageTokens[0].compareTo("createNPC") == 0){
            // create a new ghost NPC
            // Parse out the position
            System.out.println("recieved createNPC message from server");
            Vector3f ghostPosition = new Vector3f(
            Float.parseFloat(messageTokens[1]),
            Float.parseFloat(messageTokens[2]),
            Float.parseFloat(messageTokens[3]));
            try{
                createGhostNPC(ghostPosition);
            } 
            catch (IOException e) { System.out.println("Error creating ghostNPC");} 
            // error creating ghost avatar
        } 

        //MORE HERE for mnpc and isnear messages
        if (messageTokens[0].compareTo("isnr") == 0){
            //notfiy if avatar is near
            float posX, posY, posZ;
            posX = Float.parseFloat(messageTokens[1]);
            posY = Float.parseFloat(messageTokens[2]);
            posZ = Float.parseFloat(messageTokens[3]);
            Vector3f npcPos = new Vector3f(posX, posY, posZ);
            double criteria = Double.valueOf(messageTokens[4]);
            String message = "isnear" + id.toString();
            if (game.isPlayerNear(npcPos, criteria)){
                try{
                    sendPacket(message);
                }
                catch (IOException e) {e.printStackTrace();}
            }
        }

        if (messageTokens[0].compareTo("npcinfo") == 0){
            float posX, posY, posZ;
            posX = Float.parseFloat(messageTokens[1]);
            posY = Float.parseFloat(messageTokens[2]);
            posZ = Float.parseFloat(messageTokens[3]);
            Vector3f npcPos = new Vector3f(posX, posY, posZ);
            float size = Float.parseFloat(messageTokens[4]);
            if (ghostnpc == null){return;}
            ghostnpc.setPosition(npcPos);
            ghostnpc.setSize(size);
        }
    }

    public void sendJoinMessage() {
        // format: join, localId
        try{
            sendPacket(new String("join," + id.toString()));
            System.out.println("Sending join msg" + id.toString());
        } 
        catch (IOException e) { e.printStackTrace(); System.out.println("Failure in sending join msg");} 
    }

    public void sendCreateMessage(Vector3f pos){
        // format: (create, localId, x,y,z)
        try{
            String message = new String("create," + id.toString());
            message += "," + pos.get(0)+"," + pos.get(1) + "," + pos.get(2);
            sendPacket(message);
            System.out.println("sent create msg to server " + id.toString());
        }
        catch (IOException e) { e.printStackTrace();}
    }

    public void sendByeMessage() {
        try {
            String message = new String("bye," + id.toString());
            sendPacket(message);
        }
        catch (IOException e) {e.printStackTrace();}
    }

    public void sendDetailsForMessage(UUID remId, Vector3f pos) {
        try {
            String message = new String("dsfr," + remId.toString() + "," + id.toString());
            message += "," + pos.get(0)+"," + pos.get(1) + "," + pos.get(2);
            sendPacket(message);
            System.out.println("sent server my details for " + remId.toString());
        }
        catch (IOException e) {e.printStackTrace();}

    }

    public void sendMoveMessage(Vector3f pos) {
        try {
            String message = new String("move," + id.toString());
            message += "," + pos.get(0)+"," + pos.get(1) + "," + pos.get(2);
            sendPacket(message);
        }
        catch (IOException e) {e.printStackTrace();}

    }

    //---------Ghost NPC Section----------

    private void createGhostNPC(Vector3f position) throws IOException{
        if (ghostnpc == null){
            ghostnpc = new GhostNPC(0, game.getNPCShape(), game.getNPCTexture(), position);
            System.out.println("Ghost npc made");
        }
    }

    private void updateGhostNPC(Vector3f position, double gsize){ 
        float gs;
        if (ghostnpc == null){
            try{
                createGhostNPC(position);
            }
            catch (IOException e) { System.out.println("error creating npc"); }
        }

        ghostnpc.setPosition(position);
        if (gsize == 1.0f){
            gs = 1.0f;
        }
        else {gs = 2.0f;}
        ghostnpc.setSize(gs);
    }

    //MAYBE MORE HERE



}

