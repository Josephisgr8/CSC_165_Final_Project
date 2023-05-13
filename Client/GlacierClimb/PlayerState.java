package GlacierClimb;

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

import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.physics.PhysicsEngineFactory;
import tage.physics.JBullet.*;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.collision.dispatch.CollisionObject;

public abstract class PlayerState{
    PlayerAvatar player;
    String name;

    public PlayerState(PlayerAvatar playerAv){
        player = playerAv;
    }

    public abstract void toggleMove(float moveAccel);
    public abstract void jump(float jf);
    public abstract void ground();
    public abstract void airborne();
    public abstract void update();

} 

class PlayerSkatingState extends PlayerState{

    private float moveAcceleration;

    public PlayerSkatingState(PlayerAvatar playerAv, float moveAccel) {
        super(playerAv);
        this.name = "Skating State";
        moveAcceleration = moveAccel;
        this.player.animatedShape.stopAnimation();
        this.player.animatedShape.playAnimation("SKATE", player.game.AVATAR_ANIMATION_RATE, AnimatedShape.EndType.LOOP, 0);
        this.player.skatingSound.play();
    }

    @Override
    public void toggleMove(float moveAccel) {
        this.player.skatingSound.stop();
        this.player.state = new PlayerIdleState(player);
    }

    @Override
    public void jump(float jf){
        if (this.player.numOfJumps > 0) {

            float[] linV = this.player.getPhysicsObject().getLinearVelocity();
            this.player.getPhysicsObject().setLinearVelocity(new float[]{linV[0], 0f, linV[2]});
            Vector3f upward = this.player.getWorldUpVector();
            upward = upward.mul(jf);
            this.player.getPhysicsObject().applyForce(upward.x(), upward.y(), upward.z(), 0f,0f,0f);
            this.player.numOfJumps -= 1;

            this.player.skatingSound.stop();
            this.player.jumpSound.stop();
            this.player.jumpSound.play();

            this.player.state = new PlayerAirborneMovingState(player, moveAcceleration);
        }
    }

    @Override
    public void ground(){
        this.player.numOfJumps = this.player.maxJumps;
    }

    @Override
    public void airborne(){
        this.player.skatingSound.stop();
        this.player.state = new PlayerAirborneMovingState(player, moveAcceleration);
    }

    @Override
    public void update(){
        Vector3f forward = this.player.getWorldForwardVector();
        forward = forward.mul(moveAcceleration);
        this.player.getPhysicsObject().applyForce(forward.x(), forward.y(), forward.z(), 0f,0f,0f);
    }

}

class PlayerIdleState extends PlayerState{

    public PlayerIdleState(PlayerAvatar playerAv) {
        super(playerAv);
        this.name = "Idle State";
        if (this.player.animatedShape != null){
            this.player.animatedShape.stopAnimation();
        }
    }

    @Override
    public void toggleMove(float moveAccel){
        this.player.state = new PlayerSkatingState(player, moveAccel);
    }

    @Override
    public void jump(float jf){
        if (this.player.numOfJumps > 0) {
            float[] linV = this.player.getPhysicsObject().getLinearVelocity();
            this.player.getPhysicsObject().setLinearVelocity(new float[]{linV[0], 0f, linV[2]});
            Vector3f upward = this.player.getWorldUpVector();
            upward = upward.mul(jf);
            this.player.getPhysicsObject().applyForce(upward.x(), upward.y(), upward.z(), 0f,0f,0f);
            this.player.numOfJumps -= 1;

            this.player.jumpSound.stop();
            this.player.jumpSound.play();

            this.player.state = new PlayerAirborneStagnantState(player);
        }

    }

    @Override
    public void ground(){
        this.player.numOfJumps = this.player.maxJumps;
    }

    @Override
    public void airborne(){
        this.player.state = new PlayerAirborneStagnantState(player);
    }

    @Override
    public void update(){
        assert true;
    }
}

class PlayerAirborneStagnantState extends PlayerState{

    public PlayerAirborneStagnantState(PlayerAvatar playerAv){
        super(playerAv);
        this.name = "Airborne stagnant state";
        this.player.animatedShape.stopAnimation();
        this.player.animatedShape.playAnimation("FALL", player.game.AVATAR_ANIMATION_RATE, AnimatedShape.EndType.LOOP, 0);
    }

    @Override
    public void toggleMove(float moveAccel){
        this.player.state = new PlayerAirborneMovingState(player, moveAccel);
    }

    @Override
    public void jump(float jf){
        if (this.player.numOfJumps > 0) {
            float[] linV = this.player.getPhysicsObject().getLinearVelocity();
            this.player.getPhysicsObject().setLinearVelocity(new float[]{linV[0], 0f, linV[2]});
            Vector3f upward = this.player.getWorldUpVector();
            upward = upward.mul(jf);
            this.player.getPhysicsObject().applyForce(upward.x(), upward.y(), upward.z(), 0f,0f,0f);
            this.player.numOfJumps -= 1;

            this.player.jumpSound.stop();
            this.player.jumpSound.play();
        }
    }

    @Override
    public void ground(){
        this.player.numOfJumps = this.player.maxJumps;
        this.player.state = new PlayerIdleState(player);
    }

    @Override
    public void airborne(){
        assert true;
    }

    @Override
    public void update(){
        assert true;
    }

}

class PlayerAirborneMovingState extends PlayerState{

    private float moveAcceleration;

    public PlayerAirborneMovingState(PlayerAvatar playerAv, float moveAcc){
        super(playerAv);
        this.name = "Airborne Moving State";
        moveAcceleration = moveAcc * this.player.jumpMoveForceRatio;
        this.player.animatedShape.stopAnimation();
        this.player.animatedShape.playAnimation("FALL", player.game.AVATAR_ANIMATION_RATE, AnimatedShape.EndType.LOOP, 0);
    }

    @Override
    public void toggleMove(float moveAccel){
        this.player.state = new PlayerAirborneStagnantState(player);
    }

    @Override
    public void jump(float jf){
        if (this.player.numOfJumps > 0) {
            float[] linV = this.player.getPhysicsObject().getLinearVelocity();
            this.player.getPhysicsObject().setLinearVelocity(new float[]{linV[0], 0f, linV[2]});
            Vector3f upward = this.player.getWorldUpVector();
            upward = upward.mul(jf);
            this.player.getPhysicsObject().applyForce(upward.x(), upward.y(), upward.z(), 0f,0f,0f);
            this.player.numOfJumps -= 1;

            this.player.jumpSound.stop();
            this.player.jumpSound.play();
        }
        assert true;
    }

    @Override
    public void ground(){
        this.player.numOfJumps = this.player.maxJumps;
        this.player.state = new PlayerSkatingState(player, moveAcceleration/this.player.jumpMoveForceRatio);
    }

    @Override
    public void airborne(){
        assert true;
    }

    @Override
    public void update(){
        Vector3f forward = this.player.getWorldForwardVector();
        forward = forward.mul(moveAcceleration);
        this.player.getPhysicsObject().applyForce(forward.x(), forward.y(), forward.z(), 0f,0f,0f);
    }
}