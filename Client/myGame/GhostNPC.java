package myGame;

import tage.*;
import org.joml.*;

public class GhostNPC extends GameObject{

    private int id;
    public GhostNPC(int id, ObjShape s, TextureImage t, Vector3f p){
        super(GameObject.root(), s, t);
        this.id = id;
        setPosition(p);
    }

    public void setSize(float size){
        this.setLocalScale(new Matrix4f().scaling(size));
    }

    public void setPosition(Vector3f p){
        this.setLocalLocation(p);
    }
}