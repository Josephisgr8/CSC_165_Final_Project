package tage.nodeControllers;
import tage.*;
import org.joml.*;

/**
* A BounceController is a node controller that, when enabled, causes any object
* it is attached to bounce up and down based on the given height, and number of frames.
* @author Joseph DiMarino
*/

public class BounceController extends NodeController {

    private Engine engine;
    private float bounceHeight;
    private int frameAmount;
    private int currFrame;
    private boolean upDirection;

    /** Creates a BounceController with given Engine e, with bounce height h, and number of frames frms */
    public BounceController(Engine e, float h, int frms) {
        super();
        engine = e;
        bounceHeight = h;
        frameAmount = frms;
        currFrame = 0;
        upDirection = true;
    }


    /** applies the bounce mechanic to every GameObject obj in the superclass's list */
    public void apply(GameObject obj) {
        float moveAmt = bounceHeight / (float) frameAmount;

        if (currFrame > frameAmount) {upDirection = false;}
        if (currFrame < 0) {upDirection = true;}

        if (upDirection) {
            currFrame++;
            }
        if (!upDirection) {
            currFrame--;
            moveAmt *= -1;
            }

        Vector3f oldPos = new Vector3f(obj.getWorldLocation());
        Vector3f newPos = oldPos.add(new Vector3f(0f,moveAmt,0f));
        obj.setLocalLocation(newPos);

    }

}