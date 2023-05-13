package GlacierClimb;

import tage.*;
import java.util.UUID;
import org.joml.*;
import java.util.*;
import java.io.*;

public class GhostAvatar extends GameObject{
    private UUID id;
    private TextureImage snowmanT, iceCreamT;
    private ObjShape snowmanS, iceCreamS;
    
    public GhostAvatar(UUID id, ObjShape s, TextureImage t, Vector3f p){
        super(GameObject.root(), s, t);
    this.id = id;
    this.setLocalLocation(p);
    this.getRenderStates().setModelOrientationCorrection(
		(new Matrix4f()).rotationY((float)java.lang.Math.toRadians(90.0f)));
        snowmanT = t;
        snowmanS = s;
    }

    public void giveTandO(TextureImage ICT, ObjShape ICS){
        iceCreamT = ICT;
        iceCreamS = ICS;
    }

    public void changeSkin(float snowmanScale, float iceCreamScale){
        if (this.getTextureImage() == snowmanT){
            this.setTextureImage(iceCreamT);
            this.setShape(iceCreamS);
            this.setLocalScale(new Matrix4f().scaling(iceCreamScale));
            this.getRenderStates().setModelOrientationCorrection(
			(new Matrix4f()).rotationY((float)java.lang.Math.toRadians(90.0f)));
        }
        else{
            this.setTextureImage(snowmanT);
            this.setShape(snowmanS);
            this.setLocalScale(new Matrix4f().scaling(snowmanScale));
            this.getRenderStates().setModelOrientationCorrection(
			(new Matrix4f()).rotationY((float)java.lang.Math.toRadians(90.0f)));
        }
    }

    public UUID getID() {
        return id;
    }
//also need accessors and setters for id and position
}