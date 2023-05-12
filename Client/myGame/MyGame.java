package myGame;

import tage.*;
import tage.shapes.*;
import tage.input.*; 
import tage.input.action.*; 
import tage.networking.IGameConnection;
import tage.networking.IGameConnection.ProtocolType;
import tage.audio.*;

import java.lang.Math;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.UUID;
import java.net.InetAddress;
import java.util.ArrayList;
import java.net.*;
import javax.swing.*;
import org.joml.*;

import net.java.games.input.*; 
import net.java.games.input.Component.Identifier.*;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.Invocable;

import tage.physics.PhysicsEngine;
import tage.physics.PhysicsObject;
import tage.physics.PhysicsEngineFactory;
import tage.physics.JBullet.*;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.collision.dispatch.CollisionObject;


public class MyGame extends VariableFrameRateGame
{

	//INITS
	public static float ORBIT_CAM_SENSITIVITY;
	public static float AVATAR_ROTATE_SPEED;
	public static float AVATAR_ACCEL_FORCE;
	public static int AVATAR_SKATE_VOLUME;
	public static float AVATAR_SKATE_SOUND_MAX_DIST;
	public static float AVATAR_SKATE_SOUND_MIN_DIST;
	public static float AVATAR_SKATE_SOUND_ROLL_OFF;
	public static int AVATAR_JUMP_VOLUME;
	public static float AVATAR_JUMP_SOUND_MAX_DIST;
	public static float AVATAR_JUMP_SOUND_MIN_DIST;
	public static float AVATAR_JUMP_SOUND_ROLL_OFF;
	public static float AVATAR_MASS;
	public static float AVATAR_JUMP_MOVE_FORCE_RATIO;
	public static float AVATAR_JUMP_FORCE;
	public static float SPOTLIGHT_HEIGHT;
	public static float SNOWMAN_INIT_SCALE;
	public static float ICE_CREAM_INIT_SCALE;
	public static float AVATAR_ANIMATION_RATE;
	public static float MAP_SCALE_X;
	public static float MAP_SCALE_Y;
	public static float MAP_SCALE_Z;
	public static float GRAVITY;
	public static float Y_SPEED_FOR_AIRBORNE;
	public static int TERRAIN_SIZE;
	public static float GLOBAL_LIGHT_AMBIENT;
	public static float BACKGROUND_MUSIC_MAX_DIST;

	//END INITS

	private static Engine engine;
	private InputManager inputManager;

	private static ScriptEngine jsEngine;
	private PhysicsEngine physEng;
	private float vals[] = new float[16];

	public IAudioManager audioMgr;
	private Sound backgroundMusic;

	private GhostManager gm;
	private String serverAddress;
	private int serverPort, playerProgress;
	private IGameConnection.ProtocolType serverProtocol;
	public ProtocolClient protClient;
	private boolean isClientConnected = false;
	
	private double lastFrameTime, currFrameTime, elapsTime;

	private int snowyLand;    //skyboxes

	private GameObject terrainObject, ghostObj;
	private PlayerAvatar playerCharacter;
	private PhysicsObject playerCharacterPO, terrainPO;
	private ArrayList<PhysicsObject> terrainWalls = new ArrayList<PhysicsObject>();
	private ObjShape terrainShape, ghostShape, iceCreamShape, snowmanShape, npcShape;
	private AnimatedShape playerCharacterAnimatedShape, snowmanAnimatedShape, iceCreamAnimatedShape;
	private TextureImage playerCharacterTexture, terrainTexture, ghostTexture, heightMap, iceCreamTexture, snowmanTexture, npcTexture;

	private WorldBuilderHelper worldBuilder = new WorldBuilderHelper();

	private Light endLight;

	private Camera mainCam;
	private CameraOrbit3D camOrbit;

	public MyGame(String servAddr, int servPort, String prtcl) { 
		super();
		gm = new GhostManager(this);
		this.serverAddress = servAddr;
		this.serverPort = servPort;
		this.serverProtocol = ProtocolType.UDP;
	}

	public static void main(String[] args)
	{	MyGame game = new MyGame(args[0], Integer.parseInt(args[1]), args[2]);

		setupScripts(game);

		engine = new Engine(game);
		game.initializeSystem();
		game.game_loop();
	}

	@Override
	public void loadShapes()
	{	
		ghostShape = new ImportedModel("dolphinHighPoly.obj");
		npcShape = new ImportedModel("dolphinHighPoly.obj");
		iceCreamShape = new ImportedModel("IceCream.obj");
		snowmanShape = new ImportedModel("Snowman.obj");
		snowmanAnimatedShape = new AnimatedShape("Snowman.rkm", "Snowman.rks");
		snowmanAnimatedShape.loadAnimation("SKATE", "Snowman_Skating.rka");
		snowmanAnimatedShape.loadAnimation("FALL", "Snowman_Falling.rka");
		terrainShape = new TerrainPlane(TERRAIN_SIZE);
		worldBuilder.loadShapes();

	}

	@Override
	public void loadTextures()
	{	
		ghostTexture = new TextureImage("Dolphin_HighPolyUV.png");
		npcTexture = new TextureImage("Dolphin_HighPolyUV.png");
		iceCreamTexture = new TextureImage("IceCream_UV.png");
		snowmanTexture = new TextureImage("Snowman_UV.png");
		terrainTexture = new TextureImage("lake.jpg");
		//Photo by Julia Volk: https://www.pexels.com/photo/ice-on-a-frozen-lake-7099647/
		heightMap = new TextureImage("MapGrayScale.jpg");

		worldBuilder.loadTextures();
	}

	@Override
	public void loadSkyBoxes() {
		snowyLand = engine.getSceneGraph().loadCubeMap("snowyLand");
		engine.getSceneGraph().setActiveSkyBoxTexture(snowyLand);
		engine.getSceneGraph().setSkyBoxEnabled(true);
	}

	@Override
	public void buildObjects()
	{	
		createPlayerCharacter();
		buildTerrain();
		//violinPlayer = new GameObject(GameObject.root(), iceCreamShape, iceCreamTexture);
		//violinPlayer.setLocalLocation(new Vector3f(10f,0f,10f));

		worldBuilder.buildObjects();
	}

	@Override
	public void initializeLights()
	{	Light.setGlobalAmbient(GLOBAL_LIGHT_AMBIENT, GLOBAL_LIGHT_AMBIENT, GLOBAL_LIGHT_AMBIENT);
		playerCharacter.createLight(engine, SPOTLIGHT_HEIGHT);

		endLight = new Light();
		endLight.setType(Light.LightType.valueOf("POSITIONAL"));
		endLight.setLocation(new Vector3f(50f, 53f, 15f));
		endLight.setDiffuse(10f, 10f, 10f);
		endLight.setLinearAttenuation(0.3f);
		(engine.getSceneGraph()).addLight(endLight);
	}

	@Override
	public void initializeGame()
	{	lastFrameTime = System.currentTimeMillis();
		currFrameTime = System.currentTimeMillis();
		elapsTime = 0.0;
		(engine.getRenderSystem()).setWindowDimensions(1900,1000);

		// ------------- positioning the camera -------------
		mainCam = engine.getRenderSystem().getViewport("MAIN").getCamera();
		camOrbit = new CameraOrbit3D(mainCam, playerCharacter, engine,ORBIT_CAM_SENSITIVITY);

		playerProgress = 0;

		setupNetwork();
		setupAudio();
		associateActions();

		// --- initialize physics system ---
		String pEngine = "tage.physics.JBullet.JBulletPhysicsEngine";
		float[] gravity = {0f, -GRAVITY, 0f};
		physEng = PhysicsEngineFactory.createPhysicsEngine(pEngine);
		physEng.initSystem();
		physEng.setGravity(gravity);

		// --- create physics world ---
		double[ ] tempTransform;
		float[] size = {1f,1f,1f};

		Matrix4f translation = new Matrix4f(playerCharacter.getWorldTranslation());	
		tempTransform = toDoubleArray(translation.get(vals));
		playerCharacterPO = physEng.addBoxObject(physEng.nextUID(), AVATAR_MASS, tempTransform, size);
		playerCharacterPO.setBounciness(0.01f);
		playerCharacter.setPhysicsObject(playerCharacterPO);

		buildPhysicalTerrain();
		worldBuilder.buildPhysics(physEng);

		//--------Assign controls to player---------
		playerCharacter.assignControls(protClient, inputManager);
	}

	@Override
	public void update()
	{
		currFrameTime = System.currentTimeMillis();
		elapsTime = currFrameTime - lastFrameTime;
		lastFrameTime = currFrameTime;

		inputManager.update(0.0f);

		// build and set HUD
		updateHUD();

		playerCharacter.update();
		updatePhysics(elapsTime);

		//update camera
		camOrbit.updateCameraPos();

		//update sound
		setEarParams();
		
		processNetworking((float)elapsTime);
	}

	@Override
	public void keyPressed(KeyEvent e)
	{	switch (e.getKeyCode())
		{
			case KeyEvent.VK_ESCAPE:
				protClient.sendByeMessage();
				System.out.println("goodbye");
				break;

			case KeyEvent.VK_1:
				break;
			case KeyEvent.VK_2:
				break;
			case KeyEvent.VK_SPACE:
				//System.out.println(playerCharacter.getWorldLocation());
				worldBuilder.test();
				break;
		}

		super.keyPressed(e);
	}

	//--------------------------Custom Functions-------------------------//

	protected void processNetworking(float elapTime) {
		
		if (protClient != null) {
			protClient.processPackets();
		}
	}

	private class SendCloseConnectionPacketAction extends AbstractInputAction {

		//for leaving game. Need to attach input device

		@Override
		public void performAction(float time, net.java.games.input.Event e) {
			if (protClient != null && isClientConnected) {
				protClient.sendByeMessage();
			}
		}

	}

	public void setupAudio(){
		audioMgr = AudioManagerFactory.createAudioManager("tage.audio.joal.JOALAudioManager");

		if (!audioMgr.initialize()){
			System.out.println("Audio Manager failed to initialize");
			return;
		}
		setEarParams();
		playerCharacter.setupAudio();
		AudioResource violinResource = audioMgr.createAudioResource("assets/sounds/violin.wav", AudioResourceType.AUDIO_STREAM);
		//Sound Effect by <a href="https://pixabay.com/users/samuelfrancisjohnson-1207793/?utm_source=link-attribution&amp;utm_medium=referral&amp;utm_campaign=music&amp;utm_content=111391">SamuelFrancisJohnson</a> from <a href="https://pixabay.com//?utm_source=link-attribution&amp;utm_medium=referral&amp;utm_campaign=music&amp;utm_content=111391">Pixabay</a>
		backgroundMusic = new Sound(violinResource, SoundType.SOUND_MUSIC, 100, true);
		backgroundMusic.initialize(audioMgr);
		backgroundMusic.setMaxDistance(BACKGROUND_MUSIC_MAX_DIST);
		backgroundMusic.setRollOff(0f);
		backgroundMusic.play();
	}

	private void setEarParams(){
		Camera cam = mainCam;

		audioMgr.getEar().setLocation(playerCharacter.getWorldLocation());
		audioMgr.getEar().setOrientation(cam.getN(), new Vector3f(0f,1f,0f));
	}

	private void setupNetwork() {
		isClientConnected = false;

		try{
			protClient = new ProtocolClient(InetAddress.
			getByName(serverAddress), serverPort, serverProtocol, this, SNOWMAN_INIT_SCALE);
		} 
		catch (UnknownHostException e) { e.printStackTrace();}
		catch (IOException e) { e.printStackTrace(); }

		if (protClient == null){
			System.out.println("missing protocol host"); }
		else{
			// ask client protocol to send initial join message
			// to server, with a unique identifier for this client
			playerCharacter.giveClient(protClient);
			protClient.sendJoinMessage();
		}
	}

	private void createPlayerCharacter() {
		org.joml.Matrix4f initialTranslation, initialScale;

		playerCharacterAnimatedShape = snowmanAnimatedShape;
		playerCharacterTexture = snowmanTexture;

		playerCharacter = new PlayerAvatar(GameObject.root(), playerCharacterAnimatedShape, playerCharacterTexture, this);
		initialTranslation = (new org.joml.Matrix4f()).translation(0f,0f,0f);
		initialScale = (new org.joml.Matrix4f()).scaling(SNOWMAN_INIT_SCALE);
		playerCharacter.setLocalTranslation(initialTranslation);
		playerCharacter.setLocalScale(initialScale);
		playerCharacter.setAthletics(AVATAR_ACCEL_FORCE, AVATAR_JUMP_FORCE, AVATAR_JUMP_MOVE_FORCE_RATIO);
		playerCharacter.giveICSkin(iceCreamShape, iceCreamTexture);
		playerCharacter.getRenderStates().setModelOrientationCorrection(
			(new Matrix4f()).rotationY((float)java.lang.Math.toRadians(180.0f)));
	}

	private void checkForCollisions(){
		com.bulletphysics.dynamics.DynamicsWorld dynamicsWorld;
		com.bulletphysics.collision.broadphase.Dispatcher dispatcher;
		com.bulletphysics.collision.narrowphase.PersistentManifold manifold;
		com.bulletphysics.dynamics.RigidBody object1, object2;
		com.bulletphysics.collision.narrowphase.ManifoldPoint contactPoint;

		dynamicsWorld =((JBulletPhysicsEngine)physEng).getDynamicsWorld();
		dispatcher = dynamicsWorld.getDispatcher();
		int manifoldCount = dispatcher.getNumManifolds();

		for (int i=0; i<manifoldCount; i++){
			manifold = dispatcher.getManifoldByIndexInternal(i);
			object1 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody0();
			object2 = (com.bulletphysics.dynamics.RigidBody)manifold.getBody1();
			JBulletPhysicsObject obj1 = JBulletPhysicsObject.getJBulletPhysicsObject(object1);
			JBulletPhysicsObject obj2 = JBulletPhysicsObject.getJBulletPhysicsObject(object2);

			for (int j = 0; j < manifold.getNumContacts(); j++){
				contactPoint = manifold.getContactPoint(j);
				if (contactPoint.getDistance() < 0.0f){
					//System.out.println("---- hit between " + obj1 + " and " + obj2);
					if (obj1 == playerCharacter.getPhysicsObject()) {
						checkForGrounded(obj1, obj2);
					}
					break;
				} 
			} 
		}
	}

	private void checkForGrounded(PhysicsObject player, PhysicsObject obj) {

		Vector3f playerLoc, objLoc;
		Matrix4f playerMat, objMat;

		objMat = new Matrix4f();
		objMat.set(toFloatArray(obj.getTransform()));
		playerMat = new Matrix4f();
		playerMat.set(toFloatArray(player.getTransform()));

		objLoc = new Vector3f(objMat.m30(), objMat.m31(), objMat.m32());
		playerLoc = new Vector3f(playerMat.m30(), playerMat.m31(), playerMat.m32());
		
		if ((playerLoc.y() > objLoc.y()) && !(terrainWalls.contains(obj))) {
			playerCharacter.playerGrounded();
		}

	}

	private void updatePhysics(double elapsedTime) {
		Matrix4f mat = new Matrix4f();
		Matrix4f mat2 = new Matrix4f().identity();
		physEng.update((float)elapsedTime);
		checkForCollisions();

		for (GameObject go:engine.getSceneGraph().getGameObjects()){
			if (go.getPhysicsObject() != null){
				mat.set(toFloatArray(go.getPhysicsObject().getTransform()));
				mat2.set(3,0,mat.m30());
				mat2.set(3,1,mat.m31());
				mat2.set(3,2,mat.m32());
				go.setLocalTranslation(mat2);
			}
		}
	}

	private void updateHUD(){

		if (playerCharacter.getWorldLocation().y() > (float)playerProgress){
			if (playerProgress*2 != 100){
				playerProgress = (int)playerCharacter.getWorldLocation().y();
			}
		}
		String progressString = "Record progress = " + Integer.toString(playerProgress * 2) + "%";


		int hudX = (int) engine.getRenderSystem().getViewport("MAIN").getActualWidth() / 3;
        int hudY = 15;
		
		engine.getHUDmanager().setHUD1(progressString, new Vector3f(0f,0f,0f), hudX, hudY);
		engine.getHUDmanager().setHUD1font(5);
	}

	// ------------------ UTILITY FUNCTIONS used by physics
	private float[] toFloatArray(double[] arr){
		if (arr == null) return null;
		int n = arr.length;
		float[] ret = new float[n];
		for (int i = 0; i < n; i++){
			ret[i] = (float)arr[i];
		}
		return ret;
	}

	private double[] toDoubleArray(float[] arr){
		if (arr == null) return null;
		int n = arr.length;
		double[] ret = new double[n];
		for (int i = 0; i < n; i++){
			ret[i] = (double)arr[i];
		}
		return ret;
	}


	private void buildTerrain() {

		terrainObject = new GameObject(GameObject.root(), terrainShape, terrainTexture);
		terrainObject.setLocalTranslation(new org.joml.Matrix4f().translation(0f,0f,0f));
		terrainObject.setLocalScale(new org.joml.Matrix4f().scaling(
			MAP_SCALE_X, MAP_SCALE_Y,MAP_SCALE_Z));
		terrainObject.setHeightMap(heightMap);
	}

	private void buildPhysicalTerrain(){
		Matrix4f translation;
		double[] tempTransform;
		PhysicsObject tempPO;
		float up[ ] = {0,1,0};
		float tempScaleX = MAP_SCALE_X * 98 / 100;
		float tempScaleZ = MAP_SCALE_Z * 98 / 100;

		//create floor plain
		translation = new Matrix4f(terrainObject.getWorldTranslation());
		tempTransform = toDoubleArray(translation.get(vals));
		terrainPO = physEng.addStaticPlaneObject(
		physEng.nextUID(), tempTransform, up, 0.0f);
		terrainPO.setBounciness(0.01f);
		terrainObject.setPhysicsObject(terrainPO);

		//create walls
		translation = new Matrix4f().setTranslation(0f, 0f, tempScaleZ);
		tempTransform = toDoubleArray(translation.get(vals));
		tempPO = physEng.addStaticPlaneObject(
		physEng.nextUID(), tempTransform, new float[]{0f,0f,-1f}, 0.0f);
		tempPO.setBounciness(0.01f);
		tempPO.setFriction(1f);
		terrainWalls.add(tempPO);

		translation = new Matrix4f().setTranslation(tempScaleX, 0f, 0f);
		tempTransform = toDoubleArray(translation.get(vals));
		tempPO = physEng.addBoxObject(
		physEng.nextUID(), 0f, tempTransform, new float[]{0.5f, (MAP_SCALE_Y * 2) - 1f, MAP_SCALE_Z * 2});
		tempPO.setBounciness(0.01f);
		tempPO.setFriction(1f);
		terrainWalls.add(tempPO);

		translation = new Matrix4f().setTranslation(0f, 0f, -tempScaleZ);
		tempTransform = toDoubleArray(translation.get(vals));
		tempPO = physEng.addStaticPlaneObject(
		physEng.nextUID(), tempTransform, new float[]{0f,0f,1f}, 0.0f);
		tempPO.setBounciness(0.01f);
		tempPO.setFriction(1f);
		terrainWalls.add(tempPO);

		translation = new Matrix4f().setTranslation(-tempScaleX, 0f, 0f);
		tempTransform = toDoubleArray(translation.get(vals));
		tempPO = physEng.addStaticPlaneObject(
		physEng.nextUID(), tempTransform, new float[]{1f,0f,0f}, 0.0f);
		tempPO.setBounciness(0.01f);
		tempPO.setFriction(1f);
		terrainWalls.add(tempPO);

		//create physical raised terrain
		translation = new Matrix4f().setTranslation(-MAP_SCALE_X * 3/4, 0f, 0f);
		tempTransform = toDoubleArray(translation.get(vals));
		tempPO = physEng.addBoxObject(
		physEng.nextUID(), 0f, tempTransform, new float[]{MAP_SCALE_X/2, MAP_SCALE_Y/6, MAP_SCALE_Z*2});
		tempPO.setBounciness(0.01f);
		tempPO.setFriction(1f);

	}

	private void executeScript(ScriptEngine scrEng, String fileName) {

		try{ 
			FileReader fileReader = new FileReader(fileName);
			scrEng.eval(fileReader);
			fileReader.close();
		}
		catch (FileNotFoundException e1){ 
			System.out.println(fileName + " not found " + e1); }
		catch (IOException e2){
			System.out.println("IO problem with " + fileName + e2); }
		catch (ScriptException e3){
			System.out.println("ScriptException in " + fileName + e3); }
		catch (NullPointerException e4){
			System.out.println ("Null ptr exception in " + fileName + e4); }

	}

	private static void setupScripts(MyGame game) {

		ScriptEngineManager factory = new ScriptEngineManager();
		String scriptFileName = "./assets/scripts/Inits.js";

		ScriptEngine jsEngine = factory.getEngineByName("js");

		game.executeScript(jsEngine, scriptFileName);
		assignInits(jsEngine);

	}

	private void matchAvWithTerr() {
		Vector3f gbManLoc = playerCharacter.getWorldLocation();
		float terrheight = terrainObject.getHeight(gbManLoc.x(), gbManLoc.z());
		playerCharacter.setLocalLocation(new Vector3f(gbManLoc.x(), terrheight, gbManLoc.z()));
	}

	private static void assignInits(ScriptEngine jse) {

		ORBIT_CAM_SENSITIVITY = ((Double)(jse.get("ORBIT_CAM_SENSITIVITY"))).floatValue();
		SPOTLIGHT_HEIGHT = ((Double)(jse.get("SPOTLIGHT_HEIGHT"))).floatValue();
		AVATAR_ROTATE_SPEED = ((Double)(jse.get("AVATAR_ROTATE_SPEED"))).floatValue();
		AVATAR_ACCEL_FORCE = ((Double)(jse.get("AVATAR_ACCEL_FORCE"))).floatValue();
		AVATAR_SKATE_VOLUME = ((int)(jse.get("AVATAR_SKATE_VOLUME")));
		AVATAR_SKATE_SOUND_MAX_DIST = ((Double)(jse.get("AVATAR_SKATE_SOUND_MAX_DIST"))).floatValue();
		AVATAR_SKATE_SOUND_MIN_DIST = ((Double)(jse.get("AVATAR_SKATE_SOUND_MIN_DIST"))).floatValue();
		AVATAR_SKATE_SOUND_ROLL_OFF = ((Double)(jse.get("AVATAR_SKATE_SOUND_ROLL_OFF"))).floatValue();
		AVATAR_JUMP_VOLUME = ((int)(jse.get("AVATAR_JUMP_VOLUME")));
		AVATAR_JUMP_SOUND_MAX_DIST = ((Double)(jse.get("AVATAR_JUMP_SOUND_MAX_DIST"))).floatValue();
		AVATAR_JUMP_SOUND_MIN_DIST = ((Double)(jse.get("AVATAR_JUMP_SOUND_MIN_DIST"))).floatValue();
		AVATAR_JUMP_SOUND_ROLL_OFF = ((Double)(jse.get("AVATAR_JUMP_SOUND_ROLL_OFF"))).floatValue();
		AVATAR_MASS = ((Double)(jse.get("AVATAR_MASS"))).floatValue();
		AVATAR_JUMP_FORCE = ((Double)(jse.get("AVATAR_JUMP_FORCE"))).floatValue();
		AVATAR_JUMP_MOVE_FORCE_RATIO = ((Double)(jse.get("AVATAR_JUMP_MOVE_FORCE_RATIO"))).floatValue();
		SNOWMAN_INIT_SCALE = ((Double)(jse.get("SNOWMAN_INIT_SCALE"))).floatValue();
		ICE_CREAM_INIT_SCALE = ((Double)(jse.get("ICE_CREAM_INIT_SCALE"))).floatValue();
		AVATAR_ANIMATION_RATE= ((Double)(jse.get("AVATAR_ANIMATION_RATE"))).floatValue();
		MAP_SCALE_X = ((Double)(jse.get("MAP_SCALE_X"))).floatValue();
		MAP_SCALE_Y = ((Double)(jse.get("MAP_SCALE_Y"))).floatValue();
		MAP_SCALE_Z = ((Double)(jse.get("MAP_SCALE_Z"))).floatValue();
		GRAVITY = ((Double)(jse.get("GRAVITY"))).floatValue();
		Y_SPEED_FOR_AIRBORNE = ((Double)(jse.get("Y_SPEED_FOR_AIRBORNE"))).floatValue();
		TERRAIN_SIZE = ((int)(jse.get("TERRAIN_SIZE")));
		GLOBAL_LIGHT_AMBIENT = ((Double)(jse.get("GLOBAL_LIGHT_AMBIENT"))).floatValue();
		BACKGROUND_MUSIC_MAX_DIST = ((Double)(jse.get("BACKGROUND_MUSIC_MAX_DIST"))).floatValue();
	}

	private void associateActions() {
		inputManager = engine.getInputManager();
		RotateAvatarAction rotateAvatar = new RotateAvatarAction((GameObject)playerCharacter, AVATAR_ROTATE_SPEED);

		inputManager.associateActionWithAllKeyboards(
            net.java.games.input.Component.Identifier.Key.D, rotateAvatar, 
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
        inputManager.associateActionWithAllKeyboards(
            net.java.games.input.Component.Identifier.Key.A, rotateAvatar, 
            InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
	}


	//PUBLIC FUNCTIONS ---------------------------------------------------------------


	//public GameObject getAvatar() { return avatar; }

	public ObjShape getGhostShape() { return ghostShape; }
	public ObjShape getNPCShape() {return npcShape;}

	public TextureImage getGhostTexture() { return ghostTexture; }
	public TextureImage getNPCTexture() {return npcTexture;}

	public GhostManager getGhostManager() { return gm; }

	public Engine getEngine() { return engine; }

	public Vector3f getPlayerPosition() { return playerCharacter.getWorldLocation(); }

	public void setIsConnected(boolean v) {isClientConnected = v;}

	public boolean isPlayerNear(Vector3f npcPOS, double criteria){
		if (playerCharacter.getWorldLocation().distance(npcPOS) > (float)criteria){
			return false;
		}
		return true;
	}
}