package com.delmesoft.arkanoid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.delmesoft.arkanoid.input.Input;
import com.delmesoft.arkanoid.physics.World;
import com.delmesoft.arkanoid.physics.entity.Circle;
import com.delmesoft.arkanoid.physics.entity.Entity;
import com.delmesoft.arkanoid.physics.entity.Player;
import com.delmesoft.arkanoid.utils.Loop;

/*
 * Copyright (c) 2020, Sergio S.- sergi.ss4@gmail.com http://sergiosoriano.com
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *    	
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */
public abstract class Arkanoid extends Loop implements Loop.LoopListener {

	public static final int FPS = 60;
	public static final int WIDTH = 640;
	public static final int HEIGHT = 480;
	
	private final Object lock = new Object();
	
	private List<Runnable> postRunnables;
	private Map<Integer, Input> inputMap;

	private float accumulator = 0;
	private World world;

	private int level;
	private int direction;
	private Player player;

	public Arkanoid() {
		create();
		postRunnables = new ArrayList<Runnable>();
		inputMap = new HashMap<>();
		setListener(this);
	}

	private void create() {

		world = new World(WIDTH, HEIGHT) {

			@Override
			public void remove(Entity entity) {
				super.remove(entity);
				List<Entity> entities = world.getEntities();
				boolean hasBalls = false;
				boolean hasBriks = false;
				for (Entity e : entities) {
					if (e.getType() == Entity.CIRCLE) {
						hasBalls = true;
					} else if (e.getType() == Entity.RECTANGLE && !(e instanceof Player)) {
						hasBriks = true;
					}
				}

				if (!hasBriks) {
					level = (level + 1) % Levels.levels.length;
					Levels.loadLevel(Levels.levels[level], world);
				}

				if (!hasBalls) {
					createBall();
				}
			}
			
		};

		level = 0;
		Levels.loadLevel(Levels.levels[level], world);
		createBall();
		createPlayer();
	}

	private void createPlayer() {
		player = new Player(3, 100, 10);
		player.position.y = WIDTH - WIDTH * 0.7F;
		world.add(player);
	}

	private void createBall() {
		for (int i = 0; i < 1; i++) {
			Circle circle = new Circle(1, 5);
			circle.velocity.set(0, -2);
			world.add(circle);
		}
	}

	@Override
	public void update(float dt) {
		float frameTime = Math.max(dt, 0.05F);
		accumulator += frameTime;
		while (accumulator >= World.TIME_STEP) {
			world.step();
			accumulator -= World.TIME_STEP;
		}
		synchronized (lock) {
			player.force.x = direction * player.speed;
			for(Runnable runnable : postRunnables) {
				runnable.run();
			}
			postRunnables.clear();
		}
	}

	public void setDirection(int direction) {
		synchronized (lock) {
			postRunnables.add(new Runnable() {
				@Override
				public void run() {
					Arkanoid.this.direction = direction;
				}
			});
		}
	}
	
	public int getDirection() {
		return direction;
	}

	public Map<Integer, Input> getInputMap() {
		return inputMap;
	}
	
	public World getWorld() {
		return world;
	}

}
