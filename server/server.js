var S = require('string');
var fs = require('fs');
var crc = require('crc');
var async = require('async');
var http = require('http');
var request = require("request");
var httpsync = require("httpsync");
var htmlparser = require("htmlparser");
var moment = require('moment');
var Iconv  = require('iconv').Iconv;
moment.lang('de');

/* --------------------------- */
var message = "";
var examURLs = [
	{grade:"EF", time:"1. Quartal", url:"http://www.gymnasium-kamen.de/fileadmin/schule/gymnasium/HTML-Dateien/KlPl_Eph_1.Quartal_2014.htm"},
	{grade:"EF", time:"2. Quartal", url:"http://www.gymnasium-kamen.de/fileadmin/schule/gymnasium/HTML-Dateien/KlPl_Eph_2.Quartal_2014.htm"},
	{grade:"Q1", time:"1. Quartal", url:"http://www.gymnasium-kamen.de/fileadmin/schule/gymnasium/HTML-Dateien/KlPl_Q1_1._Quartal_2014.htm"},
	{grade:"Q1", time:"2. Quartal", url:"http://www.gymnasium-kamen.de/fileadmin/schule/gymnasium/HTML-Dateien/KlPl_Q1_2.Quartal_2014.htm"},
	{grade:"Q2", time:"2. Halbjahr", url:"http://www.gymnasium-kamen.de/fileadmin/schule/gymnasium/HTML-Dateien/KlPl_Q2_2014.htm"}
];
/* --------------------------- */

var debuggingInstance;
debuggingInstance = process.argv[2] == 'debug';
var substString = "{}";
var substHash = "";
var examString = "{}";
var examHash = "";
var dbg = "";

var courseLists = new Object;

//Stolen from stackoverflow
function findWithAttr(array, attr, value) {
    for(var i = 0; i < array.length; i += 1) {
        if(array[i][attr] === value) {
            return i;
        }
    }
}

tryGatherSubstData = function() {
	try {
		gatherSubstData();
	}
	catch(err) {
		console.log(err);
		tryGatherSubstData();
	}
}

function gatherSubstData(callback) {
	request("http://www.gymnasium-kamen.de/vertretungsplan.html", function(error, response, body) {
		try {
			if(body == undefined) return;
			//Preparation
			body = S(body).decodeHTMLEntities().s;
			dataObject = new Object();
			
			// --- --- PARSING --- --- //

			//Date
			body = body.split(/<H3>Vertretungsplan für (.*)<\/H3>/);
			date = body[1].split(", ")[1];
			//date = date.replace('Mrz', 'Mär'); //silly schriek
			
			offset = new Date().getTimezoneOffset() == -60 ? " +0100" : " +0200";
			dataObject.date = moment(date + offset, 'DD. MMM YYYY Z').valueOf();
			
			body = body[2];
			
			// --- Two main parts---
			topsplit = body.split(/<H3>Ersatzraumplan für .*<\/H3>/);
			
			//Part 1: Substitution & Exams
			subst = topsplit[0];
			grades = subst.split(/<TD COLSPAN=5><DIV ID="Titel">/);
			dataObject.substitution = new Array();
			dataObject.exams = new Array();
			
			var obj = {grade: "05", data:[{lesson:"1.", from:"Bitte App im Play Store aktualisieren!", to:""}]}
			dataObject.substitution.push(obj);
			var obj = {grade: "06", data:[{lesson:"1.", from:"Bitte App im Play Store aktualisieren!", to:""}]}
			dataObject.substitution.push(obj);
			var obj = {grade: "07", data:[{lesson:"1.", from:"Bitte App im Play Store aktualisieren!", to:""}]}
			dataObject.substitution.push(obj);
			var obj = {grade: "08", data:[{lesson:"1.", from:"Bitte App im Play Store aktualisieren!", to:""}]}
			dataObject.substitution.push(obj);
			var obj = {grade: "09", data:[{lesson:"1.", from:"Bitte App im Play Store aktualisieren!", to:""}]}
			dataObject.substitution.push(obj);
			
			
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
			rooms = rooms.split("</TABLE>")[0];
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
			
			substString = JSON.stringify(dataObject);
			substHash = crc.hex32(crc.crc32(substString));
			console.log(new Date().toISOString() + '   substitution data checked, crc: '+substHash);
		}
		catch(err) {
			console.log(err);
			tryGatherSubstData();
		}
	});
}

function doShit(rows) {
	var arr = new Array();
	for(var j=0; j < rows.length; j++) {
		//If only whitespace
		if(rows[j].replace(/^\s+|\s+$/g, '').length == 0) continue;
		
		rows[j] = rows[j].replace(/-----/g, "");
		rows[j] = rows[j].replace(/AUFS/g, "");
		rows[j] = rows[j].replace(/aufs/g, "");
		
		var obj2 = new Object();
		lesson = rows[j].match(/<TD><DIV ID="Eins">(<DIV ID="Adhoc">){0,1}(.*)( Std.){0,1}(<\/DIV>){0,1}<\/DIV><\/TD>/);
		lesson[2] = lesson[2].substring(0,3);
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

function gatherExamData() {
	examObject = new Array();
	
	
	var iconv = new Iconv('ISO-8859-1', 'UTF-8');
	
	async.each(examURLs, function(url, callback) {
		var handler = new htmlparser.DefaultHandler(function (error, dom) {
			if (error)
				console.log("HTML parsing error");
			else {
				var obj = new Object;
				obj.grade = url.grade;
				obj.time = url.time;
				obj.data = parseExamDOM(dom);
				examObject.push(obj);
				
				if(!(courseLists.hasOwnProperty(url.grade)))
					courseLists[url.grade] = new Array();
				
				ds = obj.data;
				for(d in ds) {
					for(c in ds[d].courses) {
						if(courseLists[obj.grade].indexOf(ds[d].courses[c]) == -1) {
							courseLists[obj.grade].push(ds[d].courses[c]);
						}
						//DONT KNOW
						//ds[d].courses[c] = courseLists[obj.grade].indexOf(ds[d].courses[c]);
					}
				}
				
				callback();
			}
		}, {ignoreWhitespace: true});
		var parser = new htmlparser.Parser(handler);
		
		var req = httpsync.get(url.url).end().data;
		data = iconv.convert(req).toString();
		parser.parseComplete(data);
	}, function(err) {
		examString = JSON.stringify({courseLists: courseLists, dates: examObject});
		examHash = crc.hex32(crc.crc32(examString));
		console.log(new Date().toISOString() + '   exam data checked, crc: '+examHash);
	});
}

function parseExamDOM(dom) {
	var ret = new Array();
	
	var base = dom[0].children[1].children[2].children;
	for(day in base)
	{
		var obj = new Object;
		
		//Is this hacky?
		if([0, base.length-1, base.length-2].indexOf(parseInt(day)) != -1)
			continue;
		
		rows = base[day].children[1].children[0].children;
		date = rows[0].children[0].children[0].children[0].children[0].data;
		date = S(date).collapseWhitespace().s
		obj.date = moment(date, 'dddd, D. MMMM YYYY').valueOf();
		obj.courses = new Array();
		for(row in rows) {
			if(row == 0) continue;
			var part1 = S(rows[row].children[0].children[0].children[0].data).collapseWhitespace().s;
			var part2 = S(rows[row].children[1].children[0].children[0].data).collapseWhitespace().s;
			obj.courses.push(part1+ ' ' + part2);
		}
		ret.push(obj);
	}

	return ret;
}

http.createServer(function (req, res) {
	if(req.headers['user-agent'] == null || req.headers['user-agent'].indexOf('VP-App') != 0) {
		str = new Date().toISOString() +'   access denied '+req.connection.remoteAddress + ' ' + req.headers['user-agent'];
		
		if(!debuggingInstance) {
			console.log(str);
			return;
		}
	}
	
	if(req.url == "/" || req.url == "/vp") {
		if(req.headers['if-none-match'] == substHash) {
			res.writeHead(304);
			res.end();
			cache = "hit";
		} else {
			res.writeHead(200, {'Content-Type': 'text/plain; charset=utf-8', 'Etag': substHash});
			res.end(substString);
			cache = "miss";
		}
		str = new Date().toISOString() +'   req '+req.connection.remoteAddress + ' ' + req.headers['user-agent'] + ' ' + cache;
	} else if(req.url == "/ep") {
		if(req.headers['if-none-match'] == examHash) {
			res.writeHead(304);
			res.end();
			cache = "hit";
		} else {
			res.writeHead(200, {'Content-Type': 'text/plain; charset=utf-8', 'Etag': examHash});
			res.end(examString);
			cache = "miss";
		}
		str = new Date().toISOString() +'   examreq '+req.connection.remoteAddress + ' ' + req.headers['user-agent'] + ' ' + cache;
	}
	
	console.log(str);

	if(!debuggingInstance)
		fs.appendFile('access.log', str + '\n', function (err) {});
	
}).listen(debuggingInstance?8082:8080, '0.0.0.0');


console.log('Server running.');

if(message != "") {
	substString = '{message:"'+message+'", "substitution":[], "rooms":[], "exams":[]}';
	substHash = crc.hex32(crc.crc32(substString));
}
else {
	tryGatherSubstData();
	setInterval(tryGatherSubstData, 10*60*1000);
	gatherExamData();
	setInterval(gatherExamData, 24*60*60*1000);
}
