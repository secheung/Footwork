package game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import object.Enemy;
import object.Enemy.EnemyState;
import object.Player.PlayerState;
import object.GameObject;
import object.Player;
import structure.ActionData;
import structure.ActionDataTool;
import structure.HitBox;
import structure.PlaneData;
import android.content.Context;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.webkit.WebView.FindListener;
import android.widget.TextView;
import engine.open2d.draw.Plane;
import engine.open2d.renderer.WorldRenderer;
import engine.open2d.text.*;
import game.GameTools.Gesture;
import game.open2d.R;

public class GameLogic extends AsyncTask<Void, Void, Void>{
	private static final int DOUBLE_TAP_INTERVAL_MIN_CHECK = 3;
	private static final int DOUBLE_TAP_INTERVAL_MAX_CHECK = 5;
	private static final int GESTURE_INTERVAL_MIN_CHECK = 2;
	private static final int GESTURE_INTERVAL_MAX_CHECK = 11;
	private static final int HOLD_INTERVAL_CHECK = 13;
	
	public enum CONTROL_TYPE{
		FIXED,
		RELATIVE
	};
	
	public static float CAM_X_CHANGE = 0.65f;
	public static float CAM_Y_CHANGE = 0.65f;
	public static float CAM_Z_CHANGE = 0.65f;
	public static float CAM_BUFFER = 0.7f;
	public static float CAM_X_DEFAULT = 0.0f;
	public static float CAM_Y_DEFAULT = 2.0f;
	public static float CAM_Z_DEFAULT = 1.0f;
	
	public static float GRAVITY = -0.098f;
	public static float FLOOR = -1.0f;
	public static float WALL_RIGHT = 7.0f;
	public static float WALL_LEFT = -7.0f;
	
	WorldRenderer worldRenderer;
	Context context;
	
	float camX = CAM_X_DEFAULT;
	float camY = CAM_Y_DEFAULT;
	float camZ = CAM_Z_DEFAULT;
	
	int enemyLimit = 1;
	int enemyIndex;
	
	CONTROL_TYPE controlType = CONTROL_TYPE.FIXED;
	boolean gameRun = false;
	
	public LinkedHashMap<String,GameObject> gameObjects;
	public LinkedHashMap<String,Label> gameLabels;
	
	float gestureX;
	float gestureY;
	int gestureCheck;
	
	Plane testBox;//for testing...duh..
	
	public GameLogic(Context context, WorldRenderer worldRenderer){
		this.worldRenderer = worldRenderer;
		this.context = context;

		worldRenderer.addCustomShader(	WorldRenderer.WORLD_SHADER,
										R.raw.vertex_shader_texture,
										R.raw.fragment_shader_texture,
										new String[]{"a_Position","a_Color","a_Normal","a_TexCoordinate","a_useTexture"}
									);
		
		gameObjects = new LinkedHashMap<String,GameObject>();
		gameLabels = new LinkedHashMap<String, Label>();
		
		ActionDataTool parser = new ActionDataTool(context);
		parser.readFile(R.raw.jack_frame_data);
		List<ActionData> playerData = parser.parseFrameData();
		
		Player player = new Player(this, playerData, 2.0f, FLOOR, controlType);
		player.prepareGameObject(worldRenderer);
		gameObjects.put(player.getName(), player);

		enemyIndex = 1;
		parser.readFile(R.raw.enemy_frame_data);
		List<ActionData> enemyData = parser.parseFrameData();
		//Enemy enemy = new Enemy(gameObjects, (Player)gameObjects.get("player"), 0, (float)(5.7f), -1.0f, 3.5f, 3.5f);
		Enemy enemy = new Enemy(this, enemyData, (Player)gameObjects.get("player"), enemyIndex, 1.0f, -1.0f);
		//enemy.getDisplay().drawDisable();
		enemy.prepareGameObject(worldRenderer);
		enemy.disableBehaviour = true;
		enemy.disableDefence = true;
		gameObjects.put(enemy.getName(), enemy);
		
		
		//enemyIndex = 2;
		//parser.readFile(R.raw.enemy_frame_data);
		//List<ActionData> enemyData2 = parser.parseFrameData();
		//Enemy enemy2 = new Enemy(this, enemyData2, (Player)gameObjects.get("player"), enemyIndex, -1.0f, -1.0f);
		//enemy2.loadAnimIntoRenderer(worldRenderer);
		//enemy2.prepareGameObject(worldRenderer);
		//enemy2.disableBehaviour = false;
		//enemy2.disableDefence = false;
		//gameObjects.put(enemy2.getName(), enemy2);
		
		
		//enemyIndex = 3;
		//parser.readFile(R.raw.enemy_frame_data);
		//List<ActionData> enemyData3 = parser.parseFrameData();
		//Enemy enemy3 = new Enemy(this, enemyData3, (Player)gameObjects.get("player"), enemyIndex, -1.5f, -1.0f);
		//enemy3.loadAnimIntoRenderer(worldRenderer);
		//enemy3.prepareGameObject(worldRenderer);
		//enemy3.disableBehaviour = false;
		//enemy3.disableDefence = false;
		//gameObjects.put(enemy3.getName(), enemy3);
		
		
		//temp control box
		Plane controlBox = new Plane("controlBox", Player.CONTROL_BOX_WIDTH, Player.CONTROL_BOX_WIDTH, 0.1f, 0.1f, 0.1f, 0.5f);
		controlBox.drawEnable();
		float[] coord = worldRenderer.getUnprojectedPoints(	worldRenderer.getScreenWidth()*Player.SCREEN_WIDTH_PERCENTAGE,
															worldRenderer.getScreenHeight()*Player.SCREEN_HEIGHT_PERCENTAGE,
															controlBox);
		controlBox.setTranslationX(coord[0]-Player.CONTROL_BOX_WIDTH/2);//offset control box by half width
		controlBox.setTranslationY(coord[1]-Player.CONTROL_BOX_HEIGHT);//offset control box by half height
		worldRenderer.addDrawShape(controlBox);
		
		//testBox = new Plane("testBox", 1.0f, 1.0f, 0.25f, 0.0f, 0.0f, 0.75f);
		//testBox.drawEnable();
		//worldRenderer.addDrawShape(testBox);
		
		gameLabels.put(player.getName()+"_pos", new Label(UUID.randomUUID().toString()));
		gameLabels.put(enemy.getName(), new Label(UUID.randomUUID().toString()));
		for(Label gameText : gameLabels.values()){
			worldRenderer.addDrawText(gameText);
		}
	}
	
	public void update(){
		if(gameObjects.size() <= enemyLimit && gameRun){
			//Enemy enemy = new Enemy(gameObjects, (Player)gameObjects.get("player"), enemyIndex, (float)(3.7f*Math.random()-7.4f*Math.random()), -1.0f, 3.5f, 3.5f);
			//enemy.loadAnimIntoRenderer(worldRenderer);
			//gameObjects.put(enemy.getName(), enemy);
			//enemyIndex++;
		}

		//interaction pre check
		for(GameObject gameObject : gameObjects.values()){
			gameObject.interactionPreCheck();
		}
		
		ArrayList<String> removeObjects = new ArrayList<String>();
		for(GameObject gameObject : gameObjects.values()){
			gameObject.update();
			if(gameObject instanceof Enemy){
				Enemy enemy = (Enemy)gameObject;
				if(enemy.getEnemyState() == EnemyState.DEAD){
					removeObjects.add(gameObject.getName());
					if(enemyLimit < 4){
						//enemyLimit++;
					}
				}
			} else if (gameObject instanceof Player){
				Player player = (Player)gameObject;
				if(player.getPlayerState() == PlayerState.DEAD){
					gameRun = false;
					enemyLimit = 1;
				}
			}
		}

		/*
		if(!gameRun){
			for(GameObject gameObject : gameObjects.values()){
				if(gameObject instanceof Enemy){
					removeObjects.add(gameObject.getName());
				}
			}
		}
		*/
		for(Object remove:removeObjects.toArray()){
			GameObject gameObject = gameObjects.get((String)remove);
			
			gameObject.unloadAnimFromRenderer(worldRenderer);
			gameObjects.remove(gameObject.getName());
//			float spawnLoc = Math.random() > 0.5 ? 1 : -1;
//			gameObject.setX(spawnLoc*3.7f);
		}
	}
	
	public void updateDrawData(){
		worldRenderer.setCamera(camX, camY, camZ);
		//worldRenderer.setOrtho(worldRenderer.getScreenWidth(),worldRenderer.getScreenHeight());
		Player player = null;
		for(GameObject gameObject : gameObjects.values()){
			gameObject.updateDrawData(worldRenderer);
			//gameObject.updateDrawText(worldRenderer);
			if(gameObject instanceof Player){
				player = (Player)gameObject;
				
				Label lab = gameLabels.get(player.getName()+"_pos");
				//float[] unProjPoint = worldRenderer.getProjectedPoint(player.getX(), player.getY()+player.getHeight(), player.getZ());
				//lab.setText("player pos "+(unProjPoint[0])+" "+(unProjPoint[1]));
				//lab.setLocation(unProjPoint[0],unProjPoint[1]);
				lab.setText("player pos "+(player.getX())+" "+(player.getY()));
				lab.setLocation(-worldRenderer.getScreenWidth()/2, worldRenderer.getScreenHeight()/4);//(0,0 is middle of screen)
			}else if(gameObject instanceof Enemy){
				Enemy enemy = (Enemy)gameObject;
				if(!gameLabels.containsKey(enemy.getName()))
					continue;
				
				Label lab = gameLabels.get(enemy.getName());
				lab.setText("enemy behaviour tick "+enemy.neutralBehaviourTick);
				lab.setLocation(-worldRenderer.getScreenWidth()/2, worldRenderer.getScreenHeight()/3);//(0,0 is middle of screen)
			}
		}
		
		/*
		if(player.isFinishState() || player.isCounterState()){
			camZoomTo(	player.getX()+player.getWidth()/2,
						player.getY()+player.getHeight()/2,
						player.getZ()-0.5f,
						CAM_BUFFER
					);
		} else {
			camZoomTo(	CAM_X_DEFAULT,
						CAM_Y_DEFAULT,
						CAM_Z_DEFAULT,
						CAM_BUFFER
				);
		}
		*/
	}

	public void camZoomTo(float x, float y, float z, float buffer){
		if(x > camX){
			camX += CAM_X_CHANGE;
		} else if (x < camX){
			camX -= CAM_X_CHANGE;
		}
		
		if(camX < x + buffer && camX > x - buffer){
			camX = x;
		}
		
		if(y > camY){
			camY += CAM_Y_CHANGE;
		} else if (y < camY){
			camY -= y;
		}
		
		if(camY < y + buffer&& camY > y - buffer){
			camY = y;
		}
		
		if(z > camZ){
			camZ += CAM_Z_CHANGE;
		} else if (z < camZ){
			camZ -= CAM_Z_CHANGE;
		}
		
		if(camZ < z + buffer&& camZ > z - buffer){
			camZ = z;
		}
		
		
	}
	
	public void finishCamZoom(Player player){
		float checkX = player.getX()+player.getWidth()/2;
		float checkY = player.getY()+player.getHeight()/2; 
		float checkZ = player.getZ();
		
		if(checkX + CAM_BUFFER > camX && checkX - CAM_BUFFER < camX){
			camX = checkX;
		} else if(checkX > camX){
			camX += CAM_X_CHANGE;
		} else if (checkX < camX){
			camX -= CAM_X_CHANGE;
		}
		
		if(checkY + CAM_BUFFER > camY && checkY - CAM_BUFFER < camY){
			camY = checkY;
		} else if(checkY > camY){
			camY += CAM_Y_CHANGE;
		} else if (checkY - CAM_BUFFER < camY){
			camY -= CAM_Y_CHANGE;
		}
		
		if (checkZ < camZ){
			camZ -= CAM_Z_CHANGE;
		}
	}
	
	//USE THIS FOR SWIPES
	public Gesture gestureProcessing(MotionEvent e, GestureListener listner){
		if(e.getAction() == MotionEvent.ACTION_DOWN){
			gestureX = e.getX();
			gestureY = e.getY();
			gestureCheck = 0;
		} else if(e.getAction() == MotionEvent.ACTION_UP){
			if(gestureCheck > GESTURE_INTERVAL_MIN_CHECK && gestureCheck < GESTURE_INTERVAL_MAX_CHECK){
				gestureCheck = 0;
				return GameTools.gestureDetection(gestureX, e.getX(), gestureY, e.getY()).setxTap(e.getX()).setyTap(e.getY());
			} else {
				gestureCheck = 0;
				return Gesture.TAP.setxTap(e.getX()).setyTap(e.getY());
			}
		} else if(e.getAction() == MotionEvent.ACTION_MOVE){
			gestureCheck++;
		}
		
		return Gesture.NONE;
	}
	
	public void passTouchEvents(MotionEvent e, GestureListener gestureListener){
		Player player = (Player) gameObjects.get(Player.OBJNAME);
		Gesture gesture = gestureProcessing(e, gestureListener);
		
		//float[] coordinates = worldRenderer.getUnprojectedPoints(e.getX(), e.getY(), testBox);
		//Log.d("coord_test","X: "+coordinates[0]+" Y: "+coordinates[1]);
		//testBox.setTranslationX(coordinates[0]-0.5f);
		//testBox.setTranslationY(coordinates[1]-0.5f);
		//worldRenderer.updateDrawObject(testBox);
		
		if(e.getAction() == MotionEvent.ACTION_DOWN){
			for(GameObject gameObject : gameObjects.values()){
				if(gameObject instanceof Enemy)
					gameObject.passTouchEvent(e, worldRenderer);
			}
			
			player.updateHoldTouchEvent(true,Gesture.HOLD.setxTap(e.getX()).setyTap(e.getY()), worldRenderer);
		} else if(e.getAction() == MotionEvent.ACTION_UP){
			gestureListener.setLongPress(false);
			if(gesture != Gesture.NONE && gesture != Gesture.TAP){
				//Log.d("rising_debug_gameLogic_passtouch",gesture.toString());
				player.passSwipeEvent(gesture, worldRenderer);
				player.saveHoldState(Gesture.HOLD_RELEASE);
				gestureListener.setdTapped(false);
			}/*else if(gestureListener.isdTapped()){//for if want swipe as precedence dtap 
				player.passDoubleTouchEvent(gestureListener, worldRenderer);
				gestureListener.setdTapped(false);
			}*/ /*else if(gesture == Gesture.TAP){
				player.setGesture(gesture);
			}*/else{
				player.updateHoldTouchEvent(false,Gesture.HOLD.setxTap(e.getX()).setyTap(e.getY()), worldRenderer);	
			}
			
			//TODO:: don't need this
			if(gesture == Gesture.SWIPE_UP){
				gameRun = true;
			}
			
		} else if(e.getAction() == MotionEvent.ACTION_MOVE){
			if(gestureCheck > HOLD_INTERVAL_CHECK){
				player.updateHoldTouchEvent(true,Gesture.HOLD.setxTap(e.getX()).setyTap(e.getY()), worldRenderer);
			}
		}
		
		player.passTouchEvent(e, worldRenderer);
	}
	
	public void passDoubleTouchEvents(GestureListener gesture){
		Player player = (Player) gameObjects.get(Player.OBJNAME);
		player.passDoubleTouchEvent(gesture, worldRenderer);
		
	}

	@Override
	protected Void doInBackground(Void... params) {
		//TODO fix up later - find method for manual renders
	    int TICKS_PER_SECOND = 30;
	    int SKIP_TICKS = 1000 / TICKS_PER_SECOND;

	    long next_game_tick = System.currentTimeMillis();

	    boolean game_is_running = true;
	    while( game_is_running ) {
	        while( System.currentTimeMillis() > next_game_tick) {
	            update();
	            updateDrawData();
	            next_game_tick += SKIP_TICKS;
	        }
	    }
	    
	    return null;
	}
}
