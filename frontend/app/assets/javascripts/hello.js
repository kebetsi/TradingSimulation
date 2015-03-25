var ws = new WebSocket("ws://localhost:9000/wsTest");
ws.onmessage = function(event) {
	console.log("WS received TrueFx quotes:");
	console.log(JSON.parse(event.data));
};
ws.onopen = function(event) { 
	console.log("WS connection open"); 
};
ws.onclose = function(event) { 
	console.log("WS connection closed"); 
};
ws.onerror = function(event) { 
	console.log("WS Error"); 
};
