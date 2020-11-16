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
var COLORS=['#07FF42', '#FF0757', '#FFF007', '#FF5407', '#07FFF6', '#9CFF07', '#FFD907', '#0774FF', '#1A07FF', '#F007FF'];
var PI2=2.0 * Math.PI;
var background;

var canvas;
window.onload = function() {

	canvas = document.getElementById('canvas');
	document.body.style.overflow = 'hidden';
	document.body.scroll = "no"; // ie only
	
	background = new Image();
	background.src = "space.jpg";

}

function onKey(keyCode, down) {
	var message = {
			type : 0,
			args : [ keyCode, down ]
	};
	sendMessage(message);
}

function onMouseMove(x, y, rect) {
	var message = {
			type : 2,
			args : [ (x - rect.left) - rect.width * 0.5, (y - rect.top) - rect.height * 0.5 ]
	};
	sendMessage(message);
}

function sendMessage(message) {
	webSocket.send(JSON.stringify(message));
}

var webSocket = new WebSocket('ws://localhost:8080/arkanoidws/websocketendpoint');

webSocket.onopen = function (event) {

	document.addEventListener('keydown', function(event) {
		onKey(event.keyCode, true);
	}, false);

	document.addEventListener('keyup', function(event) {
		onKey(event.keyCode, false);
	}, false);

	document.addEventListener('mousemove', function(event) {
		var rect = canvas.getBoundingClientRect();
		onMouseMove(event.clientX, event.clientY, rect);
	}, false);

	document.addEventListener('click', function(event) {
		var rect = canvas.getBoundingClientRect();
		onMouseMove(event.clientX, event.clientY, rect);
	}, false);

}

webSocket.onmessage = function(event) {

	var jsonObject = JSON.parse(decompress(event.data));
	
	var ctx = canvas.getContext('2d');

	var w = window.innerWidth;
	var h = window.innerHeight;

	ctx.canvas.width  = w;
	ctx.canvas.height = h;
	
	var ar;
	if(w > h) {
		ar = (h / (jsonObject.h * 1.5));
	} else {
		ar = (w / (jsonObject.w * 1.5));
	}
	
/*	ctx.fillStyle="#F2FBFF";
	ctx.fillRect(0, 0, w, h);*/

	ctx.translate(w * 0.5, h * 0.5);
	ctx.drawImage(background, -jsonObject.w * 0.5 * ar, -jsonObject.h * 0.5 * ar, jsonObject.w * ar, jsonObject.h * ar);
		
	var data = jsonObject.d;
	
	var len = data.length;
	for(var i = 0; i < len;) {
		
		var c = data[i];
		var x = data[i + 1] * ar;
		var y = data[i + 2] * ar;
		ctx.fillStyle = COLORS[c];
		if(data[i + 3] == 0) {
			drawCircle(ctx, x, y, data[i + 4] * ar);
			ctx.fill();			
			i += 5;
		} else {
			var hw = data[i + 4] * ar;
			var hh = data[i + 5] * ar;
			ctx.fillRect(x - hw, y - hh, hw * 2.0, hh * 2.0);
			i+= 6;
		}		
		
	}

	ctx.stroke();

}

function decompress(base64data) {

	// Decode base64 (convert ascii to binary)
	var strData = atob(base64data);

	// Convert binary string to character-number array
	var charData= strData.split('').map(function(x){return x.charCodeAt(0);});

	// Turn number array into byte-array
	var binData = new Uint8Array(charData);

	// Pako inflate
	return pako.inflate(binData, { to: 'string' });
}

function drawCircle(ctx, x, y, r) {

	ctx.beginPath();

	var n = Math.max(1, Math.floor(6 * Math.cbrt(r))) - 1;
	var angle = PI2 / (n + 1);

	var cos = Math.cos(angle);
	var sin = Math.sin(angle);

	var cx = r, cy = 0;
	ctx.lineTo(r + x, y);
	for(var i = 0; i < n; i++) {		
		var temp = cx;
		cx = cos * cx   - sin * cy;
		cy = sin * temp + cos * cy
		ctx.lineTo(cx + x, cy + y);
	}
	ctx.lineTo(r + x, y);
	ctx.closePath();

}
