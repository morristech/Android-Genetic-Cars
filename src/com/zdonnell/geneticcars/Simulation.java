package com.zdonnell.geneticcars;

import android.util.Log;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class Simulation implements ApplicationListener {

	private static final int GENERATION_SIZE = 10;

    OrthographicCamera camera;

	private Renderer renderer;

	protected World world;

	private List<Body> terrainTiles = new ArrayList<Body>();

	private List<Car> activeCars = new ArrayList<Car>(GENERATION_SIZE);

    private List<Car> deadCars = new ArrayList<Car>(GENERATION_SIZE);

    private float aspect = 0.5f;

    public void setAspect(float aspect) {
        if (camera != null)
            camera.setToOrtho(false, 12, 12 * aspect);

        this.aspect = aspect;
    }

    @Override
    public void create() {
        // create the camera and the SpriteBatch
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 12, 12 * aspect);
		camera.position.set(0, 0, 0);

		// create the world
		world = new World(new Vector2(0, -9.8f), true);
		ShapeRenderer shapeRenderer = new ShapeRenderer();
		renderer = new Renderer(shapeRenderer);

		terrainTiles = TerrainGenerator.generate(world);
		createGeneration();
    }

    @Override
    public void render() {
		long startTime = System.currentTimeMillis();
        world.step(Gdx.app.getGraphics().getDeltaTime(), 3, 3);
        long worldStepTime = System.currentTimeMillis() - startTime;
        Log.d("GENETIC CARS:", "WORLD STEP TIME: " + worldStepTime);

		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		Gdx.gl.glEnable(GL10.GL_BLEND);
		Gdx.gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

		Gdx.gl.glLineWidth(3);
		camera.update();

		Car leadCar = determineLeadCar();

		Vector3 position = camera.position;
		position.x += (leadCar.getChassis().getPosition().x - position.x) * 0.2f;
		position.y += (leadCar.getChassis().getPosition().y - position.y) * 0.2f;

		findDeadCars();
		if (activeCars.isEmpty())
			createGeneration();
        //dbrenderer.render(world, camera.combined);

		renderer.setProjectionMatrix(camera.combined);
		renderer.renderCars(activeCars);
		renderer.renderTiles(terrainTiles);
	}

	/**
	 * Looks through the list of active cars to determine which
	 * are still "alive."
	 */
	private void findDeadCars() {
		ListIterator<Car> iter = activeCars.listIterator();
		// Iterate through each active car and see if it has surpassed it's previous
		// max distance.  If it hasn't in the last 3 seconds, kill it :(
		while (iter.hasNext()) {
			Car car = iter.next();
			if (car.getChassis().getPosition().x > car.maxDistance) {
				car.maxDistance = car.getChassis().getPosition().x;
				car.timeLastMoved = System.currentTimeMillis();
			} else {
				if (System.currentTimeMillis() - car.timeLastMoved > 5000) {
					iter.remove();
					car.removeFromWorld(world);
                    deadCars.add(car);
				}
			}
		}
	}

    @Override
    public void dispose() {
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


	private void createGeneration() {
		for (int i = 0; i < GENERATION_SIZE; i++) {
			Car newCar = CarFactory.buildCar(new CarDefinition(), world);
			activeCars.add(newCar);
		}
	}

	private Car determineLeadCar() {
		Car leadCar = activeCars.get(0);
		for (Car car : activeCars) {
			float leadCarDist = (leadCar == null) ? 0 : leadCar.getChassis().getPosition().x;
			float curCarDist = car.getChassis().getPosition().x;
			if (curCarDist > leadCarDist) {
				leadCar = car;
			}
		}
		return leadCar;
	}
}