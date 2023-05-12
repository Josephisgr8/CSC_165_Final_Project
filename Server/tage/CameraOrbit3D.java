package tage;

import org.joml.*;
import java.lang.Math;
import tage.input.*; 
import tage.input.action.*;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;
import net.java.games.input.*; 
import net.java.games.input.Component.Identifier.*;

/**
* A CameraOrbit3D is a specific camera that allows the user to revolve the camera around a GameObject.
* A CameraOrbit3D is constructed with a given Camera, GameObject, Engine, and sensitivity.
* The default location is directly behind the GameObject, 5 units behind, and 30 degrees upward.
* By default, the controls are hard coded, with UP, DOWN, LEFT, RIGHT, E, and Q keys to control the
* revolving and the zooming.
* It contains 3 private classes, and 1 public method, updateCameraPos().
* @author Joseph DiMarino
*/

public class CameraOrbit3D {
    private Engine engine;
    private Camera camera;
    private GameObject subject;
    private float camAzimuth;
    private float camElevation;
    private float camRadius;
    private float sensitivity;


    /** Instantiates a CameraOrbit3D using the given Camera, looking at the given GameObject, using the
    given engine, with the given sensitivity. */
    public CameraOrbit3D(Camera cam, GameObject obj, Engine e, float sens) {
        engine = e;
        camera = cam;
        subject = obj;
        sensitivity = sens;
        
        camAzimuth = 0.0f;
        camElevation = 30.0f;
        camRadius = 10.0f;

        setInputs();

    }

    private void setInputs() {
        OrbitAzimuthAction azmAction = new OrbitAzimuthAction(sensitivity);
        OrbitElevationAction eleAction = new OrbitElevationAction(sensitivity);
        OrbitRadiusAction radAction = new OrbitRadiusAction(sensitivity);
        InputManager im = engine.getInputManager();

        im.associateActionWithAllKeyboards(
            net.java.games.input.Component.Identifier.Key.RIGHT, azmAction,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(
            net.java.games.input.Component.Identifier.Key.LEFT, azmAction,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(
            net.java.games.input.Component.Identifier.Key.UP, eleAction,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(
            net.java.games.input.Component.Identifier.Key.DOWN, eleAction,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(
            net.java.games.input.Component.Identifier.Key.E, radAction,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        im.associateActionWithAllKeyboards(
            net.java.games.input.Component.Identifier.Key.Q, radAction,
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
    }

    /** updates the camera's position in the world based on the azimuth, elevation, and radius. */
    public void updateCameraPos() {
        Vector3f subjectRotation = subject.getWorldForwardVector();
        double subjectAzAngle = Math.toDegrees((double) subjectRotation.angleSigned(
            new Vector3f(0,0,-1), new Vector3f(0,1,0)));

        double subjectElAngle = Math.toDegrees((double) subjectRotation.angleSigned(
            new Vector3f(0,1,0), new Vector3f(0,1,0)));

        float totalAz = camAzimuth - (float)subjectAzAngle;
        float totalEl = camElevation - (float)subjectElAngle;
        double theta = Math.toRadians(totalAz); 
        double phi = Math.toRadians(totalEl); 
        float x = camRadius * (float)(Math.cos(phi) * Math.sin(theta)); 
        float y = camRadius * (float)(Math.sin(phi)); 
        float z = camRadius * (float)(Math.cos(phi) * Math.cos(theta)); 
        camera.setLocation(new 
            Vector3f(x,y,z).add(subject.getWorldLocation())); 
        camera.lookAt(subject);
    }

    private class OrbitAzimuthAction extends AbstractInputAction {
        private float rotAmount;

        public OrbitAzimuthAction(float amt) {
            rotAmount = amt;
        }

        @Override
        public void performAction(float time, Event e) {

            if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Key.RIGHT) {
                camAzimuth += rotAmount;
            }
            else if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Key.LEFT) {
                camAzimuth -= rotAmount;
            }

            camAzimuth = camAzimuth % 360;
            updateCameraPos();
        }
    }

    private class OrbitElevationAction extends AbstractInputAction {
        private float rotAmount;

        public OrbitElevationAction(float amt) {
            rotAmount = amt;
        }

        @Override
        public void performAction(float time, Event e) {

            if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Key.UP) {
                camElevation += rotAmount;
            }
            else if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Key.DOWN) {
                camElevation -= rotAmount;
            }

            camElevation = camElevation % 360;
            if (camElevation < 5) {camElevation = 5;}
            if (camElevation > 80) {camElevation = 80;} 
            updateCameraPos();
            //System.out.println(camElevation);
        }
    }

    private class OrbitRadiusAction extends AbstractInputAction {
        private float incrementAmount;

        public OrbitRadiusAction(float amt) {
            incrementAmount = amt / 10f;
        }

        @Override
        public void performAction(float time, Event e) {
            if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Key.E) {
                camRadius += incrementAmount; 
            }
            else if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Key.Q) {
                camRadius -= incrementAmount;
            }

            updateCameraPos();

        }
    }
}