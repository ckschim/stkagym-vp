var fs = require('fs');


fs.readFile('access.log', {encoding:"utf-8"}, function (err, data) {
	if (err) throw err;
	data = data.split('\n');
	data.pop();
	emit(analyze(parse(data)));
});


var parse = function(data) {
	var stats = new Array();
	
	for(req in data) {
		var obj = new Object();
		
		first = data[req].split("   ");
		obj.time = new Date(first[0]);
		
		second = first[1].split(" ");
		obj.type = second[0];
		obj.ip = second[1];
		obj.version = second[2];
		obj.grade = second[3];
		obj.cache = second[4];
		
		stats.push(obj);
	}
	return stats;
}

var analyze = function(data){
	//console.log(data);
	cleaned = data.filter(function(req, index, arr) {
		if(index == 0) return true;
		
		for(i = index-1; i >= 0; i--) {
			if(req.time - arr[i].time > 30*1000) 
				return true;
			else if (arr[index-1].ip == req.ip)
				return false;
		}
		
		return true;
	});
	
	//Counting
	
	var counts_ip = {};
	var counts_grade = {"05":0, "06":0, "07":0, "08":0, "09":0, "EF":0, "Q1":0, "Q2":0};
	//var counts_time = {"0-1":0,"1-2":0,"2-3":0,"3-4":0,"4-5":0,"5-6":0,"6-7":0,"7-8":0,"8-9":0,"9-10":0,"10-11":0,"11-12":0,"12-13":0,"13-14":0,"14-15":0,"15-16":0,"16-17":0,"17-18":0,"18-19":0,"19-20":0,"20-21":0,"21-22":0,"22-23":0,"23-24":0};
	counts_time = {}
	var counts_cache = {};
	for (var i = 0; i < cleaned.length; i++) {
		//Count unique IPs
		counts_ip[cleaned[i].ip] = 1 + (counts_ip[cleaned[i].ip] || 0);
		
		//Count grades
		counts_grade[cleaned[i].grade.substring(0,2)] = 1 + (counts_grade[cleaned[i].grade.substring(0,2)] || 0);
		
		//Count times
		time = cleaned[i].time.getHours() + ':' + (Math.floor(cleaned[i].time.getMinutes()/30)==0?'00':'30') + '-' + (parseInt(cleaned[i].time.getHours()*6 + Math.floor(cleaned[i].time.getMinutes()/10))+1);
		counts_time[time] = 1 + (counts_time[time] || 0);
		
		//Count cache hits
		counts_cache[cleaned[i].cache] = 1 + (counts_cache[cleaned[i].cache] || 0);
	}
	
	return {total:data.length, cleaned:cleaned.length, unique:Object.keys(counts_ip).length, grades:counts_grade, time:counts_time, cache:counts_cache};
}


var emit = function(stats) {
	console.log("Total: "+stats.total);
	console.log("Cleaned: "+stats.cleaned);
	console.log("Unique: "+stats.unique);
	keys = Object.keys(stats.grades);
	arr = []
	for(k in keys) {
		arr.push([keys[k], stats.grades[keys[k]]]);
	}
	console.log("Grades: "+JSON.stringify(arr));
	keys = Object.keys(stats.time);
	arr = []
	for(k in keys) {
		arr.push([parseInt(keys[k]), stats.time[keys[k]]]);
	}
	/*for(k in arr) {
		arr[k] = [Math.floor(arr[k][0]/2) + ":" + (arr[k][0] % 2)*30, arr[k][1]] 
	}*/
	console.log("Times: "+JSON.stringify(arr));
	console.log("Cache states: "+JSON.stringify(stats.cache));
}
