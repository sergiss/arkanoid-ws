package com.delmesoft.arkanoid.physics.entity;

import com.delmesoft.arkanoid.physics.World;
import com.delmesoft.arkanoid.physics.broadphase.AABB;
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
public abstract class Entity extends AABB {
		
	public static final int CIRCLE    = 0;
	public static final int RECTANGLE = 1;
	
	public final Vec2 position;
	public final Vec2 velocity;
	
	public World world;
	
	public int color;
	
	public float bounce;
	
	public boolean remove;
	
	public Vec2 force;
	
	public Entity (int color) {
		this.color = color;
		
		position = new Vec2();
		velocity = new Vec2();
		force = new Vec2();
		
		bounce = 0.25F;
	}
	
	public void update() {
		
		position.add(velocity);
		updateAABB();
		
		if (world.min.x > min.x) {
			position.x += world.min.x - min.x;
			velocity.x *= -bounce;
			updateAABB();
		} else if (world.max.x < max.x) {
			position.x += world.max.x - max.x;
			velocity.x *= -bounce;
			updateAABB();
		}
		
		if (world.min.y > min.y) {
			position.y += world.min.y - min.y;
			velocity.y *= -bounce;
			updateAABB();
		} else if (world.max.y < max.y) {
			position.y += world.max.y - max.y;
			velocity.y *= -bounce;
			updateAABB();
			if(this instanceof Circle) {
				remove = true;
			}
		}
		
	}
	
	public abstract void updateAABB();
	
	public abstract int getType();

}
