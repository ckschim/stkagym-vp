var S = require('string');
var fs = require('fs');
var crc = require('crc');
var http = require('http');
var request = require("request");
var moment = require('moment');
moment.lang('de');

/* --------------------------- */
var message = "";
/* --------------------------- */


var dataString = "{}";
var dataHash = "";
var dbg = "";

//Stolen from stackoverflow
function findWithAttr(array, attr, value) {
    for(var i = 0; i < array.length; i += 1) {
        if(array[i][attr] === value) {
            return i;
        }
    }
}

gatherData = function(callback) {
	request("http://www.gymnasium-kamen.de/vertretungsplan.html", function(error, response, body) {
		//Preparation
		body = S(body).decodeHTMLEntities().s;
		dataObject = new Object();
		
		// --- --- PARSING --- --- //

		//Date
		body = body.split(/<H3>Vertretungsplan für (.*)<\/H3>/);
		date = body[1].split(", ")[1];
		dataObject.date = moment(date + " +0200", "DD. MMM YYYY Z").valueOf();
		body = body[2];
		
		// --- Two main parts---
		topsplit = body.split(/<H3>Ersatzraumplan für .*<\/H3>/);
		
		//Part 1: Substitution & Exams
		subst = topsplit[0];
		grades = subst.split(/<TD COLSPAN=5><DIV ID="Titel">/);
		dataObject.substitution = new Array();
		dataObject.exams = new Array();
		
		for(i in grades) {
			if(i == 0) continue;
			var obj = new Object();
			obj.grade = grades[i].split("<")[0];
			
			rows = grades[i].split("<TR>");
			rows.splice(0, 1);
			obj.data = doShit(rows);
			
			if(obj.grade.substring(0, 1) == "K") {
				obj.grade = obj.grade.substring(1, obj.grade.length-1);
				
				index = findWithAttr(dataObject.exams, "grade", obj.grade);
					
				if(index != undefined) {
					dataObject.exams[index].data = dataObject.exams[index].data.concat(obj.data);
				}
				else 
					dataObject.exams.push(obj);
			}
			else
				dataObject.substitution.push(obj);
		}
		
		//Part 2: Rooms
		rooms = topsplit[1];
		rooms = rooms.replace(/<TR>/g, "");
		grades = rooms.split(/<TD COLSPAN=5><DIV ID="Titel">/);
		dataObject.rooms = new Array();
		for(i in grades) {
			if(i == 0) continue;
			var obj = new Object();
			obj.grade = grades[i].split("<")[0];
			
			rows = grades[i].split("</TR>");
			rows.splice(0, 1);
			rows.pop();
			obj.data = doShit(rows);
			
			dataObject.rooms.push(obj);
		}
		
		dataString = JSON.stringify(dataObject);
		dataHash = crc.hex32(crc.crc32(dataString));
		console.log(new Date().toISOString() + '   data checked, crc: '+dataHash);
	});
}

var doShit = function(rows) {
	var arr = new Array();
	for(var j=0; j < rows.length; j++) {
		//If only whitespace
		if(rows[j].replace(/^\s+|\s+$/g, '').length == 0) continue;
		
		rows[j] = rows[j].replace(/-----/g, "");
		rows[j] = rows[j].replace(/AUFS/g, "");
		rows[j] = rows[j].replace(/aufs/g, "");
		
		var obj2 = new Object();
		lesson = rows[j].match(/<TD><DIV ID="Eins">(<DIV ID="Adhoc">){0,1}(.*) Std.(<\/DIV>){0,1}<\/DIV><\/TD>/);
		if(lesson[2].substring(0,1) == "0")
			lesson[2] = lesson[2].substring(1,lesson[2].length);
		obj2.lesson = lesson[2];
		
		from = rows[j].match(/<TD><DIV ID="Zwei">(<DIV ID="Adhoc">){0,1}([^<]*)(<\/DIV>){0,1}<\/DIV><\/TD>/);
		obj2.from = from[2];
		
		to1 = rows[j].match(/<TD><DIV ID="Vier">(<DIV ID="Adhoc">){0,1}([^<]*)(<\/DIV>){0,1}<\/DIV><\/TD>/);
		to2 = rows[j].match(/<TD><DIV ID="Fuenf">(<DIV ID="Adhoc">){0,1}([^<]*)(<\/DIV>){0,1}<\/DIV><\/TD>/);
		obj2.to = to1[2] + " " + to2[2];
		
		arr.push(obj2);
	}
	return arr;
}

http.createServer(function (req, res) {
	if(req.headers['if-none-match'] == dataHash) {
		res.writeHead(304);
		res.end();
		cache = "hit"
	}
	else {
		res.writeHead(200, {'Content-Type': 'text/plain; charset=utf-8', 'Etag': dataHash});
		res.end(dataString);
		cache = "miss"
	}
	
	str = new Date().toISOString() +'   req '+req.connection.remoteAddress + ' ' + req.headers['user-agent'] + ' ' + cache;
	console.log(str);
	fs.appendFile('access.log', str + '\n', function (err) {});
	
}).listen(8080, '0.0.0.0');


console.log('Server running at http://0.0.0.0:8080/');
if(message != "") {
	dataString = '{message:"'+message+'"}';
	dataHash = crc.hex32(crc.crc32(dataString));
}
else {
	gatherData();
	setInterval(gatherData, 10*60*1000);
}
