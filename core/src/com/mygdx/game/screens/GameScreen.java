package com.mygdx.game.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.mygdx.game.MyGame;
import com.mygdx.game.sprites.Ball;
import com.mygdx.game.sprites.Bricks;
import com.mygdx.game.sprites.Paddle;
import static com.mygdx.game.util.Constants.*;


public class GameScreen implements Screen {

    private MyGame myGame;
    private ShapeRenderer shapeRenderer;
    private Bricks[] bricks;
    private Array<Bricks> brickses;
    private Paddle gamePaddle;
    private Ball gameBall;
    private float ballXmove;
    private float ballYmove;
    //the number of player lives
    private int lives = GAME_LIVES;
    //the number of destroyed bricks
    private int bricksDestroyed;
    //division of the paddle to three parts
    private int aPaddlePart;
    private int bPaddlePart;
    private int cPaddlePart;
    private int numOfBricks;

    public Texture paddleImage;
    public Texture brickImage;
    public Texture ballImage;

    private Sound hitSound;
    private Sound loseSound;
    private Sound hitPaddleSound;
    private Music music;

    private OrthographicCamera cam;
    Application.ApplicationType appType;


    public GameScreen (MyGame myGame){
        this.myGame = myGame;
        gamePaddle = new Paddle(this);
        gameBall = new Ball(this);

        bricks = new Bricks[NUM_OF_BRICKS];
        numOfBricks = 0;
        brickses = new Array<Bricks>();
        //division of the paddle to three parts
        aPaddlePart = (int)gamePaddle.getPaddle().x;
        bPaddlePart = (int)gamePaddle.getPaddle().x + 21;
        cPaddlePart = (int)gamePaddle.getPaddle().x + 40;

        // load the drop sound effect and the background music
        hitSound = Gdx.audio.newSound(Gdx.files.internal("sound/hit.wav"));
        hitPaddleSound = Gdx.audio.newSound(Gdx.files.internal("sound/pop.wav"));
        loseSound = Gdx.audio.newSound(Gdx.files.internal("sound/lose.wav"));
        music = Gdx.audio.newMusic(Gdx.files.internal("sound/music.mp3"));
        music.setLooping(true);

        // create the camera
        cam = new OrthographicCamera(HEIGHT, WIDTH);
        cam.setToOrtho(false, HEIGHT, WIDTH);

        shapeRenderer = new ShapeRenderer();
        shapeRenderer.setProjectionMatrix(cam.combined);

        //for the accelerometer
        appType = Gdx.app.getType();

        ballXmove = MathUtils.random(-300, 300);
        ballYmove = 300;


        makeBricks();

    }

    @Override
    public void show() {
        // start the playback of the background music
        // when the screen is shown
        music.play();
    }

    @Override
    public void render (float delta) {
        Gdx.gl.glClearColor(0, 0, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // tell the camera to update its matrices.
        cam.update();

        // tell the SpriteBatch to render in the
        // coordinate system specified by the camera.
        myGame.batch.setProjectionMatrix(cam.combined);

        // begin a new batch and draw the paddle, ball and
        // all bricks
        myGame.batch.begin();
        myGame.font.draw(myGame.batch, "Bricks Destroyed: " + bricksDestroyed, 0, 480);
        myGame.font.draw(myGame.batch, "Lives Left: " + lives, 700, 480);
        myGame.batch.draw(paddleImage, gamePaddle.getPaddle().x, gamePaddle.getPaddle().y);
        for (int i = 0; i < NUM_OF_BRICKS; i++) {
            if (!bricks[i].isDestroyed()) {
                myGame.batch.draw(brickImage, bricks[i].getBrick().x, bricks[i].getBrick().y);
            }
        }
        myGame.batch.draw(ballImage, gameBall.getBall().x, gameBall.getBall().y);
        myGame.batch.end();

        gameControls(delta);
        ballWallCollision();
        ballMovement(delta);
        paddleScreenBounds();
        debugRenderer();
        ballHitBrick();
        ballHitPaddle();
        checkBallSpeed();
        isGameDone();
    }

    private void gameControls(float delta){
        // Accelerometer and keyboard controls
        if (appType == Application.ApplicationType.Android || appType == Application.ApplicationType.iOS) {
            gamePaddle.getPaddle().x += Gdx.input.getAccelerometerY() * PADDLE_SPEED * delta;
        } else {
            // process user input
            if (Gdx.input.isTouched()) {
                Vector3 touchPos = new Vector3();
                touchPos.set(Gdx.input.getX(), Gdx.input.getY(), 0);
                cam.unproject(touchPos);
                gamePaddle.getPaddle().x = touchPos.x - 64 / 2;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT))
                gamePaddle.getPaddle().x -= PADDLE_SPEED * delta;
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT))
                gamePaddle.getPaddle().x += PADDLE_SPEED * delta;
        }
    }

    private void isGameDone(){
        //if we lose all game lives go back to main screen
        if (lives <=0){
            myGame.setScreen(new MainMenuScreen(myGame,"false"));
            music.stop();
        }
        //we destroyed all the bricks so we put true for the boolean in the constructor
        if (numOfBricks < 1)
            myGame.setScreen(new MainMenuScreen(myGame, "true"));
    }

    //the direction of the ball after it hits the game borders
    //if goes below the paddle we lose a life, we
    //hear the lose sound and the ball starts over
    //the initial position

    private void ballWallCollision () {


        if (gameBall.getBall().x < 0|| gameBall.getBall().x > myGame.x) {
            //Here we flip the speed, so it bonces the other way.
            ballXmove *= -1;
        }

        if (gameBall.getBall().y >myGame.y) {
            ballYmove *= -1;
        }
        if (gameBall.getBall().y < 0) {
            gameBall.initial();
            loseSound.play();
            lives--;
        }
    }

    private void ballMovement(float delta){
        gameBall.getBall().x += ballXmove * delta;
        gameBall.getBall().y += ballYmove * delta;
    }

    private void paddleScreenBounds(){
        // make sure the paddle stays within the screen bounds
        if (gamePaddle.getPaddle().x < 1)
            gamePaddle.getPaddle().x = 1 ;
        if (gamePaddle.getPaddle().x > 800 - 64)
            gamePaddle.getPaddle().x = 800 - 64;
    }

    private void makeBricks(){
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 10; j++) {
                bricks[numOfBricks] = new Bricks(this, j * 70 + 60, i * 20 + 360);
                numOfBricks++;
            }
        }
    }

    private void debugRenderer(){
        // Tells shapeRenderer to draw an outline of the following shapes
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        // Chooses RGB Color of 255, 109, 120 at full opacity
        shapeRenderer.setColor(255 / 255.0f, 109 / 255.0f, 120 / 255.0f, 1);

        for (int i=0; i < NUM_OF_BRICKS; i++) {
            //Draws the brick if it is not destroyed
            if (!bricks[i].isDestroyed()) {
                shapeRenderer.rect(bricks[i].getBrick().x, bricks[i].getBrick().y,
                        bricks[i].getBrick().width, bricks[i].getBrick().height);
            }
        }
        shapeRenderer.end();
    }

    //the direction of the ball when hits a brick
    //the brick dies and we hear the crash sound
    private void ballHitBrick(){
        for (int i = 0; i < NUM_OF_BRICKS; i++) {
            if (Intersector.overlaps(gameBall.getBall(), bricks[i].getBrick())) {
                if (!bricks[i].isDestroyed()) {
                    bricks[i].setDestroyed();
                    bricksDestroyed++;
                    ballYmove *= -1;
                    hitSound.play();
                    numOfBricks--;
                }
            }
        }
    }

    // the direction of the ball when it hits the paddle
    //which depends on the part of the paddle it hits
    private void ballHitPaddle(){
        if (Intersector.overlaps(gameBall.getBall(), gamePaddle.getPaddle())) {
            if ((gameBall.getBall().x) >= aPaddlePart && (gameBall.getBall().x) < bPaddlePart){
                ballYmove *= -1;
                ballXmove -=150;
            }
            else if ((gameBall.getBall().x) >= bPaddlePart && (gameBall.getBall().x) < cPaddlePart){
                ballYmove *= -1;
            }
            else{
                ballYmove *= -1;
                ballXmove +=150;
            }
            hitPaddleSound.play();
        }
    }

    // limits the ball speed
    private void checkBallSpeed(){
        if (ballYmove > BALL_SPEED)
            ballYmove = BALL_SPEED;
        if (ballYmove < -BALL_SPEED)
            ballYmove = -BALL_SPEED;

        if (ballXmove > BALL_SPEED)
            ballXmove = BALL_SPEED;
        if (ballXmove < BALL_SPEED)
            ballXmove = -BALL_SPEED;

    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        ballImage.dispose();
        paddleImage.dispose();
        brickImage.dispose();
        hitSound.dispose();
        music.dispose();

    }
}