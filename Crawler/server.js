//server.js
var db = require('./dbinterface');
var express = require('express.io');
var app     = express();
app.http().io();

app.use(express.static('public'));

app.get('/' , function( req, res ){
	res.send('GET request to the homepage');
});


app.io.route('DB' , {
	frontier: function(req){
		//console.log("The client asked for the frontier!")
		db.frontier_count(function(err, data){
			if(err){
				console.log(err);
			}
			else{
				req.io.emit('frontier' , { count: data });
			}
		});
	},
	explored: function(req){
		//console.log("The cliend asked for the exlored!")
		db.explored_count(function(err, data){
			if(err){
				console.log(err);
			}
			else{
				req.io.emit('explored' , { count: data });
			}
		});
	}
});

var host = '0.0.0.0';
var port = 8003;
if( process.argv.length == 3) {
	host = process.argv[2];
}
if( process.argv.length == 4 ) {
	host = process.argv[2];
	port = process.argv[3];
}
var server = app.listen(port,host ,function() {
	var host = server.address().address;
	var port = server.address().port;
	console.log("Crawler Report server running at http://%s:%s" , host , port);
});


function broadCastFrontier(){
	//console.log("BroadCasting Frontier");
	db.frontier_count(function(err, data){
			if(err){
				console.log(err);
			}
			else{
					app.io.broadcast( 'frontier' , { count: data} );
			}
		});
}

function broadCastExplored(){
	//console.log("BroadCastign explored");
	db.explored_count(function(err, data){
			if(err){
				console.log(err);
			}
			else{
				app.io.broadcast('explored' , { count: data });
			}
		});
}

function periodicUpdate(){
	broadCastFrontier();
	broadCastExplored();
}

function tick(){
	periodicUpdate();
	setTimeout(tick , 5000);
}

tick();

