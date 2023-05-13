package GlacierClimb;

import tage.*;
import java.util.UUID;
import org.joml.*;
import java.util.*;
import java.io.*;

public class GhostManager{
    private MyGame game;
    private Vector<GhostAvatar> ghostAvs = new Vector<GhostAvatar>();

    public GhostManager(VariableFrameRateGame vfrg){
        game = (MyGame)vfrg;
    }

    public void createGhost(UUID id, Vector3f p, float initScale) throws IOException{
        ObjShape s = game.getSnowmanShape();
        TextureImage t = game.getSnowmanTexture();
        GhostAvatar newAvatar = new GhostAvatar(id, s, t, p);
        Matrix4f initialScale = (new Matrix4f()).scaling(initScale);
        newAvatar.setLocalScale(initialScale);
        ghostAvs.add(newAvatar);
        System.out.println("success in creating ghost");
        newAvatar.giveTandO(game.getIceCreamTexture(), game.getIceCreamShape());
    }

    public void removeGhostAvatar(UUID id){
        System.out.println("Removing ghost avatar");
        GhostAvatar ghostAv = findAvatar(id);
        if(ghostAv != null){
            game.getEngine().getSceneGraph().removeGameObject(ghostAv);
            ghostAvs.remove(ghostAv);
            System.out.println("removed ghost avatar");
        }
        else{
            System.out.println("unable to find ghost in list");
        } 
    }

    private GhostAvatar findAvatar(UUID id){
        GhostAvatar ghostAvatar;
        Iterator<GhostAvatar> it = ghostAvs.iterator();
        while(it.hasNext()) {
            ghostAvatar = it.next();
            if(ghostAvatar.getID().compareTo(id) == 0){
                return ghostAvatar;
            } 
        }
        return null;
    }

    public void updateGhostAvatar(UUID id, Vector3f position) throws IOException{
        GhostAvatar ghostAvatar = findAvatar(id);
        if (ghostAvatar != null) { ghostAvatar.setLocalLocation(position); }
    } 

    public void rotateGhostAvatar(UUID id, float rotateAmount) throws IOException{
        GhostAvatar ghostAvatar = findAvatar(id);
        if (ghostAvatar != null){ghostAvatar.yaw(rotateAmount);}
    }

    public void changeSkin(UUID id) throws IOException{
        GhostAvatar ghostAvatar = findAvatar(id);
        if (ghostAvatar != null){ghostAvatar.changeSkin(game.SNOWMAN_INIT_SCALE, game.ICE_CREAM_INIT_SCALE);}
    }
}
