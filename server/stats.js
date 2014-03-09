var fs = require('fs');
var http = require('http');
var url = require('url');
var crypto = require('crypto');

var page = fs.readFileSync("statspage.html");
var data = fs.readFileSync("access.log", {encoding:"utf-8"});

var cache = fs.readFileSync("cache.json");
	cache = cache == "" ? "[]" : cache;
	cache = JSON.parse(cache);

var parse = function(data) {
	/*Timer*/ var start = new Date();
	
	data = data.split('\n');
	data.pop();
	
	
	for(var req = cache.length; req < data.length; req++) {
		first = data[req].split("   ");
		var stamp = new Date(first[0]);
		var obj = new Object();
		
		obj.time = stamp;
		
		second = first[1].split(" ");
		obj.type = second[0];
		obj.ip = second[1];
		obj.version = second[2];
		obj.grade = second[3];
		obj.cache = second[4];
		
		cache.push(obj);
	}
	//cache.concat(append);
	setTimeout(function() {
		fs.writeFile("cache.json", JSON.stringify(cache));
	}, 0);
	
	/*Timer output*/ console.log("parse " + (new Date().getTime() - start.getTime()));
	return cache;
}

var countByDay = function(stats) {
	/*Timer*/ var start = new Date();
	ret = {}
	ret.start = stats[0].time;
	ret.end = stats[stats.length-1].time;
	ret.data = []
	j = 0
	
	for(i in stats) {
		if(i != 0 && stats[i-1].time.getDay() != stats[i].time.getDay()) {
			j++;
		}
		
		ret.data[j] = 1 + (ret.data[j] || 0)
	}
	
	/*Timer output*/ console.log("countByDay " + (new Date().getTime() - start.getTime()));
	return ret;
};
var cnt = 0;
var clean = function(data) {
	/*Timer*/ var start = new Date();
	
	cleaned = data.filter(function(req, index, arr) {
		req.time = new Date(req.time);
		//console.log("filtering");
		if(index == 0) return true;
		
		for(i = index-1; i >= 0; i--) {
			if(req.time - arr[i].time > 20*1000) 
				return true;
			else if (arr[index-1].ip == req.ip /*&& arr[index-1].grade == req.grade*/)
				return false;
		}
		
		return true;
	});
	
	/*Timer output*/ console.log("clean " + (new Date().getTime() - start.getTime()));
	return cleaned;
}

var analyze = function(from, to, data) {
	/*Timer*/ var start = new Date();
	ret = {};
	from = new Date(parseInt(from));
	to = new Date(parseInt(to));
	
	reqs = []
	
	for(i in cleaned) {
		if(cleaned[i].time < from) { continue;console.log('skip');}
		if(cleaned[i].time > to){ break;console.log('break');}
		reqs.push(cleaned[i]);
	}
	
	//Counting
	
	var counts_ip = {};
	var counts_grade = {"05":0, "06":0, "07":0, "08":0, "09":0, "EF":0, "Q1":0, "Q2":0};
	var counts_time = [[],[],[],[],[],[],[]];
	var counts_version = {};
	var counts_cache = {};
	for (var i = 0; i < reqs.length; i++) {
		//Count unique IPs
		counts_ip[reqs[i].ip] = 1 + (counts_ip[reqs[i].ip] || 0);
		
		//Count grades
		counts_grade[reqs[i].grade.substring(0,2)] = 1 + (counts_grade[reqs[i].grade.substring(0,2)] || 0);
		
		//Count versions
		counts_version[reqs[i].version] = 1 + (counts_version[reqs[i].version] || 0);
		
		//Count times
		
		time = reqs[i].time.getHours()*2 + Math.floor(reqs[i].time.getMinutes() / 30);
		day = reqs[i].time.getDay();
		counts_time[day][time] = 1 + (counts_time[day][time] || 0);
		
		//Count cache hits
		counts_cache[reqs[i].cache] = 1 + (counts_cache[reqs[i].cache] || 0);
	}
	
	var grades_array = [];
	for(i in counts_grade) {
		grades_array.push({name:i, y:counts_grade[i]});
	}
	
	var version_array = [];
	for(i in counts_version) {
		version_array.push({name:i, y:counts_version[i]});
	}
	
	/*Timer output*/ console.log("analyze " + (new Date().getTime() - start.getTime()));
	return {cleaned:reqs.length, unique:Object.keys(counts_ip).length, grades:grades_array, time:counts_time, cache:counts_cache, version:version_array};
}

http.createServer(function (req, res) {
	
	var queryData = url.parse(req.url, true);
	
	if(queryData.pathname != "/data") {
		page = fs.readFileSync("statspage.html");
		res.writeHead(200, {'Content-Type': 'text/html; charset=utf-8'});
		res.end(page);
	}
	
	res.writeHead(200, {'Content-Type': 'text/plain; charset=utf-8'});
	
	if(queryData.query.master != null) {
		res.end(JSON.stringify(countByDay(clean(parse(fs.readFileSync("access.log", {encoding:"utf-8"}))))));
	}
	
	if(queryData.query.from != null && queryData.query.to != null) {
		/*Timer*/ var start = new Date();
		str = JSON.stringify(analyze(queryData.query.from, queryData.query.to, clean(parse(fs.readFileSync("access.log", {encoding:"utf-8"})))));
		res.end(str);
		/*Timer output*/ console.log("request took " + (new Date().getTime() - start.getTime()));
	}
	
	res.end("invalid request");
	
}).listen(8081, '0.0.0.0');


console.log('Server running.');
