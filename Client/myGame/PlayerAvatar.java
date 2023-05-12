package myGame;

import tage.*;
import tage.shapes.*;
import tage.input.*; 
import tage.input.action.*; 
import tage.networking.IGameConnection;
import tage.networking.IGameConnection.ProtocolType;
import tage.input.action.AbstractInputAction;
import net.java.games.input.Event;

import java.lang.Math;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.UUID;
import java.net.InetAddress;
import java.net.*;
import javax.swing.*;
import org.joml.*;
import tage.audio.*;

import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.physics.PhysicsEngineFactory;
import tage.physics.JBullet.*;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.collision.dispatch.CollisionObject;

public class PlayerAvatar extends GameObject {
    protected PlayerState state;
    public MyGame game;
    public ProtocolClient protClient;
    public float moveForce, jumpForce, jumpMoveForceRatio;
    public int numOfJumps;
    public int maxJumps = 30;
    public AnimatedShape animatedShape;

	public Sound skatingSound;

    private Light playerSpotlight;
    private float spotlightHeight;

    public PlayerAvatar(GameObject root, ObjShape shape, TextureImage texture, MyGame g) {
        super(root, shape, texture);
        numOfJumps = maxJumps;
        game = g;
        //protClient = game.protClient;
        state = new PlayerIdleState(this);
    }

    public PlayerAvatar(GameObject root, AnimatedShape anShape, TextureImage texture, MyGame g){
        super(root, anShape, texture);
        numOfJumps = maxJumps;
        game = g;
        //protClient = game.protClient;
        state = new PlayerIdleState(this);
        animatedShape = anShape;
    }

    public void giveClient(ProtocolClient p) {
        protClient = p;
    }

    public void setAthletics(float mf, float jf, float jmfr){
        this.moveForce = mf;
        this.jumpForce = jf;
        this.jumpMoveForceRatio = jmfr;
    }

    public void createLight(Engine engine, float h){
        spotlightHeight = h;

        playerSpotlight = new Light();
		playerSpotlight.setType(Light.LightType.valueOf("SPOTLIGHT"));
		playerSpotlight.setLocation(new Vector3f(5.0f, 4.0f, 2.0f));
		playerSpotlight.setDirection(new Vector3f(0f, -1f, 0f));
        playerSpotlight.setAmbient(0.5f,0.5f,0.5f);
		(engine.getSceneGraph()).addLight(playerSpotlight);
    }

    public void setupAudio(){
        AudioResource skateResource;

        skateResource = game.audioMgr.createAudioResource("assets/sounds/SKATE.wav", AudioResourceType.AUDIO_SAMPLE);
		//Sound Effect from <a href="https://pixabay.com/?utm_source=link-attribution&amp;utm_medium=referral&amp;utm_campaign=music&amp;utm_content=32721">Pixabay</a>

        skatingSound = new Sound(skateResource, SoundType.SOUND_EFFECT, game.AVATAR_SKATE_VOLUME, true);

		skatingSound.initialize(game.audioMgr);
		skatingSound.setMaxDistance(game.AVATAR_SKATE_SOUND_MAX_DIST);
		skatingSound.setMinDistance(game.AVATAR_SKATE_SOUND_MIN_DIST);
		skatingSound.setRollOff(game.AVATAR_SKATE_SOUND_ROLL_OFF);

		skatingSound.setLocation(this.getWorldLocation());

    }


    public void assignControls(ProtocolClient client, InputManager inputManager){
        MoveAvatarAction moveAvatar = new MoveAvatarAction(this);
        AvatarJumpAction jumpAvatar = new AvatarJumpAction(this);

        inputManager.associateActionWithAllKeyboards(
            net.java.games.input.Component.Identifier.Key.W, moveAvatar,
            InputManager.INPUT_ACTION_TYPE.ON_PRESS_AND_RELEASE);
        inputManager.associateActionWithAllKeyboards(
            net.java.games.input.Component.Identifier.Key.S, moveAvatar,
            InputManager.INPUT_ACTION_TYPE.ON_PRESS_AND_RELEASE);
        inputManager.associateActionWithAllKeyboards(
            net.java.games.input.Component.Identifier.Key.SPACE, jumpAvatar,
            InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);
    }

    public void update(){
        super.update();
        checkForAirborne();
        if (this.animatedShape != null){
            this.animatedShape.updateAnimation();
            updateState();
        }
        updateSoundLocation();
        if (this.getPhysicsObject() == null) { System.out.println("This PO is null");}
        updateLight();
    }

    private void updateLight(){
        if (this.playerSpotlight != null){
            playerSpotlight.setLocation(new Vector3f(this.getWorldLocation().x, 
                this.getWorldLocation().y + spotlightHeight,
                this.getWorldLocation().z));
        }
    }

    private float[] toFloatArray(double[] arr){
		if (arr == null) return null;
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++){
			ret[i] = (float)arr[i];
		}
		return ret;
	}

    public void playerGrounded(){
        this.state.ground();
    }

    public void playerAirborne(){
        this.state.airborne();
    }

    protected void moveForward(float moveAcc){
        this.state.toggleMove(moveAcc);
    }

    protected void jump(){
        this.state.jump(jumpForce);
    }

    protected void changeSkin(){
        //code
    }

    private void updateSoundLocation(){
        if (this.skatingSound != null)
        {
            this.skatingSound.setLocation(this.getWorldLocation());
        }
    }

    private void updateState(){
        this.state.update();
    }

    private void checkForAirborne(){
        float[] v = new float[3];
        if (this.getPhysicsObject() != null){
            v = this.getPhysicsObject().getLinearVelocity();
        }

        if (Math.abs(v[1]) > game.Y_SPEED_FOR_AIRBORNE) {
            playerAirborne();
        }
    }

    
}

class MoveAvatarAction extends AbstractInputAction{

    private PlayerAvatar subject;
    private float moveAmount;
    private ProtocolClient ptclCl;

    public MoveAvatarAction(PlayerAvatar sub) {

        subject = sub;
        moveAmount = subject.moveForce;
        ptclCl = subject.protClient;

    }

    @Override
    public void performAction(float time, Event e) {
        
        if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Key.W) {
            subject.moveForward(moveAmount);
        }
        if (e.getComponent().getIdentifier() == net.java.games.input.Component.Identifier.Key.S) {
            subject.moveForward(-moveAmount);
        }

        if (ptclCl != null) {
            ptclCl.sendMoveMessage(subject.getWorldLocation());
        }
        if (ptclCl == null) {System.out.println("ProtocolClient is null");}
    }
}

class AvatarJumpAction extends AbstractInputAction{

    private PlayerAvatar subject;

    public AvatarJumpAction(PlayerAvatar sub) {
        subject = sub;
    }

    @Override
    public void performAction(float time, Event e) {
        subject.jump();
    }
}

class AvatarChangeSkinAction extends AbstractInputAction{

    private PlayerAvatar subject;

    public AvatarChangeSkinAction(PlayerAvatar sub){
        subject = sub;
    }

    @Override
    public void performAction(float time, Event e){
        subject.changeSkin();
    }


}