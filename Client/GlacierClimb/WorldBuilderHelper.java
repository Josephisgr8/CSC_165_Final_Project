package GlacierClimb;

import tage.*;
import tage.shapes.*;
import org.joml.*;
import java.util.*;

import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.physics.PhysicsEngineFactory;
import tage.physics.JBullet.*;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.collision.dispatch.CollisionObject;

public class WorldBuilderHelper{
    private ArrayList<GameObject> blockLong = new ArrayList<GameObject>();
    private GameObject endPlatform;

    private ObjShape blockLongShape;
    private TextureImage blockLongTexture;

    private ArrayList<PhysicsObject> blockLongPO = new ArrayList<PhysicsObject>();
    private PhysicsObject endPlatformPO;

    public WorldBuilderHelper(){
        assert true;
    }

    public void loadShapes(){
        blockLongShape = new ImportedModel("block_long.obj");
    }

    public void loadTextures(){
        blockLongTexture = new TextureImage("block_long_UV.png");
    }

    public void buildObjects(){
        addLongBlock(0f,10f,28f);
        addLongBlock(15f,15f,28f);
        addLongBlock(26f,20f,20f);
        addLongBlock(26f,24f,10f);
        addLongBlock(26f,28f,0f);
        addLongBlock(26f,32f,-10f);

        //corner platform
        addLongBlock(26f,36f,-28f);
        addLongBlock(26f,36f,-24f);
        addLongBlock(18f,36f,-28f);
        addLongBlock(18f,36f,-24f);

        //lower corner platform
        addLongBlock(-26f,28f,-28f);
        addLongBlock(-26f,28f,-24f);
        addLongBlock(-18f,28f,-28f);
        addLongBlock(-18f,28f,-24f);

        addLongBlock(-26f,34f,0f);
        addLongBlock(0f,36f,0f);
        addLongBlock(0f,41f,15f);
        addLongBlock(26f,49f,15f);

        //End platform
        endPlatform = new GameObject(GameObject.root(), blockLongShape, blockLongTexture);
        endPlatform.setLocalLocation(new Vector3f(50f, 45f, 15f));
        endPlatform.setLocalScale(new Matrix4f().scaling(5.0f));
    }

    public void buildPhysics(PhysicsEngine p){
        for (GameObject block : blockLong){
            addLongBlockPO(p, block);
        }

        //End platform
        double[] tempTransform;
        float vals[] = new float[16];
        float[] endPlatformSize = new float[]{40f, 9f, 20f};

        Matrix4f translation = new Matrix4f(endPlatform.getWorldTranslation());
        tempTransform = toDoubleArray(translation.get(vals));
        endPlatformPO = p.addBoxObject(p.nextUID(), 0f, tempTransform, endPlatformSize);
        endPlatformPO.setBounciness(0.01f);
    }

    //assists in building physics objects
    private double[] toDoubleArray(float[] arr){
		if (arr == null) return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++){
			ret[i] = (double)arr[i];
		}
		return ret;
	}

    private void addLongBlock(float x, float y, float z){
        blockLong.add(new GameObject(GameObject.root(), blockLongShape, blockLongTexture));
        blockLong.get(blockLong.size() - 1).setLocalLocation(new Vector3f(x,y,z));
    }

    private void addLongBlockPO(PhysicsEngine p, GameObject block){
        double[] tempTransform;
        float vals[] = new float[16];
        float[] blockLongSize = new float[]{8f, 1f, 4f};

        Matrix4f translation = new Matrix4f(block.getWorldTranslation());
        tempTransform = toDoubleArray(translation.get(vals));
        blockLongPO.add(p.addBoxObject(p.nextUID(), 0f, tempTransform, blockLongSize));
        blockLongPO.get(blockLongPO.size() - 1).setBounciness(0.01f);
    }
}