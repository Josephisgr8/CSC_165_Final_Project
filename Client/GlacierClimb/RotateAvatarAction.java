package GlacierClimb;

import tage.*;
import tage.shapes.*;
import java.lang.Math;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import org.joml.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

public class RotateAvatarAction extends AbstractInputAction {

    private GameObject subject;
    private float rotateAmount;

    public RotateAvatarAction(GameObject sub, float amt) {

        subject = sub;
        rotateAmount = amt;

    }

    public void performAction(float time, Event e) {

        if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Key.D) {
            subject.yaw(-rotateAmount); 
        }
        if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Key.A) {
            subject.yaw(rotateAmount);
        }
        if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Axis.X){
            float keyValue = e.getValue();
            if (keyValue > 0.3f) {
                subject.yaw(-rotateAmount);
            }
            if (keyValue < -0.3f) {
                subject.yaw(rotateAmount);
            }
        }
    }

}