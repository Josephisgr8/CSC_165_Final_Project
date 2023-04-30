package myGame;

import tage.*;
import java.util.UUID;
import org.joml.*;
import java.util.*;
import java.io.*;

public class GhostAvatar extends GameObject{
    private UUID id;
    
    public GhostAvatar(UUID id, ObjShape s, TextureImage t, Vector3f p){
        super(GameObject.root(), s, t);
    this.id = id;
    this.setLocalLocation(p);
    }

    public UUID getID() {
        return id;
    }
//also need accessors and setters for id and position
}