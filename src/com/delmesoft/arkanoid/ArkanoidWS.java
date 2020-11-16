package com.delmesoft.arkanoid;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.delmesoft.arkanoid.input.Input;
import com.delmesoft.arkanoid.input.InputAdapter;
import com.delmesoft.arkanoid.input.InputListener;
import com.delmesoft.arkanoid.physics.entity.Circle;
import com.delmesoft.arkanoid.physics.entity.Entity;
import com.delmesoft.arkanoid.physics.entity.Rectangle;
import com.delmesoft.arkanoid.utils.Compressor;
import com.delmesoft.httpserver.Session;
import com.delmesoft.httpserver.websocket.WebSocketHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
public class ArkanoidWS extends WebSocketHandler {
	
	private final Executor executor;
	
	private final Object lock;
	private final Gson gson;
	private final HashMap<Long, Arkanoid> arkanoidMap;
	
	public ArkanoidWS() {
		lock = new Object();
		gson =  new GsonBuilder().create();
		arkanoidMap = new HashMap<Long, Arkanoid>();
		executor = Executors.newFixedThreadPool(2);
		super.getIndexSet().add("/arkanoidws/websocketendpoint");
	}
	
	@Override
	public void onOpen(Session session) {

		System.out.println(String.format("Session opened %s", session.toString()));

		synchronized (lock) {
			Arkanoid arkanoid = new Arkanoid() {
				final Compressor compressor = new Compressor();
				@Override
				public void render() {
					// Send game state to client
					List<Entity> entities = getWorld().getEntities();
					int n = entities.size() - 1;
					StringBuilder sb = new StringBuilder();
					sb.append('[');
					if (n > -1) {
						Entity entity;
						for (int i = 0; i < n; i++) {
							entity = entities.get(i);
							append(sb, entity);
							sb.append(',');
						}
						append(sb, entities.get(n));
					}
					sb.append(']');
					final String json = String.format("{\"w\":%d,\"h\":%d,\"d\":%s}", (int) getWorld().getWidth(), (int) getWorld().getHeight(), sb.toString());
					executor.execute(new Runnable() {
						@Override
						public void run() {
							try {
								byte[] data = compressor.compress(json.getBytes());
								String text = Base64.getEncoder().encodeToString(data);
								// System.out.println("Compression: " + ((float)
								// text.length() / json.length()));
								sendText(text, session);
							} catch (Throwable e) {
								closeSession(session);
							}
						}
					});
				}
			};
			
			Input input = new Input(37);
			input.setInputListener(new InputAdapter() {
				@Override
				public void onStateChange(boolean down) {
					if (down) {
						arkanoid.setDirection(-1);
					} else if (arkanoid.getDirection() < 0) {
						arkanoid.setDirection(0);
					}
				}
			});
			arkanoid.getInputMap().put(input.getCode(), input);
			input = new Input(39);
			input.setInputListener(new InputAdapter() {
				@Override
				public void onStateChange(boolean down) {
					if (down) {
						arkanoid.setDirection(1);
					} else if (arkanoid.getDirection() > 0) {
						arkanoid.setDirection(0);
					}
				}
			});
			arkanoid.getInputMap().put(input.getCode(), input);

			arkanoidMap.put(session.getId(), arkanoid);
			arkanoid.start();
		}
						
	}
	
	private void append(StringBuilder sb, Entity entity) {
		sb.append(entity.color).append(',');
		sb.append((int) entity.position.x).append(',');
		sb.append((int) entity.position.y).append(',');
		sb.append(entity.getType()).append(',');
		if (entity.getType() == Entity.CIRCLE) {
			sb.append((int) ((Circle) entity).radius);
		} else {
			sb.append((int) ((Rectangle) entity).halfWidth).append(',');
			sb.append((int) ((Rectangle) entity).halfHeight);
		}		
	}

	@Override
	public void onClose(Session session) {
		Arkanoid arkanoid = arkanoidMap.remove(session.getId());
		if(arkanoid != null) {
			arkanoid.stop();
		}
	}

	@Override
	public void onData(byte[] data, int len, Session session) {}

	@Override
	public void onText(String text, Session session) {
		try {

			Message message = gson.fromJson(text, Message.class);
			List<Object> args = message.getArgs();

			switch (message.getType()) {
			case Message.KEY_PRESSED: { // only keys
				int keyCode = ((Number) args.get(0)).intValue();
				boolean down = (boolean) args.get(1);
				synchronized (lock) {
					Input input = arkanoidMap.get(session.getId()).getInputMap().get(keyCode);
					if (input != null && input.isDown() != down) {
						input.setDown(down);
						InputListener listener = input.getInputListener();
						if (listener != null) {
							listener.onStateChange(down);
						}
					}
				}
				break;
			}
//			case Message.MOUSE_MOVED: {
//				int x = ((Number) args.get(0)).intValue();
//				int y = ((Number) args.get(1)).intValue();
//				synchronized (lock) {
//					Input input = arkanoidMap.get(session.getId()).getInputMap().get(-1);
//					if (input != null) {
//						InputListener listener = input.getInputListener();
//						if (listener != null) {
//							listener.onStateChange(x, y);
//						}
//					}
//				}
//				break;
//			}
			}

		} catch(Throwable e) {
			closeSession(session);
		}
	}
	
	public void closeSession(Session session) {
		synchronized (lock) {
			Arkanoid arkanoid = arkanoidMap.remove(session.getId());
			if (arkanoid != null) {
				try {
					arkanoid.stop();
					session.close();
				} catch (IOException e) {}
			}
		}
	}

}
