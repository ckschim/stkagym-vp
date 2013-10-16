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
	var counts_grade = {};
	var counts_time = {};
	var counts_cache = {};
	for (var i = 0; i < cleaned.length; i++) {
		//Count unique IPs
		counts_ip[cleaned[i].ip] = 1 + (counts_ip[cleaned[i].ip] || 0);
		
		//Count grades
		counts_grade[cleaned[i].grade] = 1 + (counts_grade[cleaned[i].grade] || 0);
		
		//Count times
		time = cleaned[i].time.getHours() + '-' + (parseInt(cleaned[i].time.getHours())+1);
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
	console.log("Grades: "+JSON.stringify(stats.grades));
	console.log("Times: "+JSON.stringify(stats.time));
	console.log("Cache states: "+JSON.stringify(stats.cache));
}
