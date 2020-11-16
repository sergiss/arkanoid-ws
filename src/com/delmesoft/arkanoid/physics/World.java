package com.delmesoft.arkanoid.physics;

import java.util.ArrayList;
import java.util.List;

import com.delmesoft.arkanoid.Arkanoid;
import com.delmesoft.arkanoid.physics.broadphase.AABB;
import com.delmesoft.arkanoid.physics.entity.Circle;
import com.delmesoft.arkanoid.physics.entity.Entity;
import com.delmesoft.arkanoid.physics.entity.Player;
import com.delmesoft.arkanoid.physics.entity.Rectangle;
import com.delmesoft.arkanoid.utils.Vec2;

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
public class World extends AABB {
	
	public static final float FRICTION  = 0.95f;

	public static final float TIME_STEP = 1F / Arkanoid.FPS;
	
	private final List<Entity> entities;
	private final Vec2 normal = new Vec2();
	private final Vec2 incidence = new Vec2();	
	
	public World(float width, float height) {
		super(width, height);
		entities = new ArrayList<>();
	}

	public void step() {
		
		Entity entity;
		Circle ball = (Circle) entities.get(0);
		int i, n = entities.size();
		for (i = 1; i < n; i++) {
			entity = entities.get(i);
			if(!entity.remove && entity.overlap(ball)) {
				float penetration = handleCollision(ball, (Rectangle) entity, normal);
				if(penetration > 0) {
					ball.position.addScl(normal, -penetration);
					float speed = ball.velocity.len();
					if (entity instanceof Player) {							
						incidence.set(-ball.velocity.x - entity.velocity.x, -ball.velocity.y).nor();
					} else {							
						incidence.set(-ball.velocity.x, -ball.velocity.y).nor();
						entity.remove = true;
					}
					float dot = incidence.dot(normal);
					ball.velocity.set(2 * normal.x * dot - incidence.x, 2 * normal.y * dot - incidence.y);
					ball.velocity.scl(speed);
					break;
				}
			}
		}
		
		for (i = 0; i < n; i++) {
			entity = entities.get(i);
			if (entity.remove) {
				remove(entity);
				i--; n--;
			} else {
				if(entity.getType() != Entity.CIRCLE) {
					// World friction (velocity)
					entity.velocity.scl(FRICTION);
					if(entity instanceof Player) {
						// Apply force
						entity.velocity.addScl(entity.force, TIME_STEP);
						entity.force.setZero(); // clear force
					}
				}
				entity.update();				
			}
		}
		
	}
	
	private float handleCollision(Circle circle, Rectangle rectangle, Vec2 normal) {
		float dx = rectangle.position.x - circle.position.x; 
		float dy = rectangle.position.y - circle.position.y;
		float cx = dx < -rectangle.halfWidth  ? -rectangle.halfWidth  : (dx > rectangle.halfWidth  ? rectangle.halfWidth  : dx);
		float cy = dy < -rectangle.halfHeight ? -rectangle.halfHeight : (dy > rectangle.halfHeight ? rectangle.halfHeight : dy);
		float d;
		if(dx == cx && dy == cy) {
			float absX = Math.abs(dx);
			float absY = Math.abs(dy);
			// Find closest axis
			if(absX > absY) {
				// Clamp to closest extent
				if(cx > 0)
					cx =  rectangle.halfWidth;
				else 
					cx = -rectangle.halfWidth;
			} else { // y axis is shorter
				// Clamp to closest extent
				if(cy > 0)
					cy =  rectangle.halfHeight;
				else
					cy = -rectangle.halfHeight;
			}
			dx = cx - dx;
			dy = cy - dy;
			d = (float) StrictMath.sqrt(dx * dx + dy * dy);
		} else {
			dx -= cx;
			dy -= cy;
			d = dx * dx + dy * dy;
			if(d > circle.radius * circle.radius) {
				return 0;
			}
			d = (float) StrictMath.sqrt( d );
		}
		normal.set(dx, dy).nor();
		return circle.radius - d;
	}

	public void add(Entity entity) {
		entity.world = this;
		if (entity instanceof Circle)
			entities.add(0, entity);
		else
			entities.add(entity);
		entity.updateAABB();
	}
	
	public void remove(Entity entity) {
		entities.remove(entity);
	}
	
	public List<Entity> getEntities() {
		return entities;
	}
	

}
