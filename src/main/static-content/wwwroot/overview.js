/*
*
* Developed by Songjie Wang
* Department of EECS
* University of Missouri
*
*/


window.onload= function(){

	var numUsers;
	var resource = "EEG sensor";
	var secondsAgo = 5;
	var data=[];
	var chart = new CanvasJS.Chart("chartContainer", {
			title: {
				text: "Overview of Emotion Measurements"
			},
			axisY: {
				title: "Emotion level",
				suffix: ""
			},
			legend: {
			       cursor:"pointer",
			       //fontSize: 15,
			        itemclick: legendClick
			     },
			data:[]
		});	
		
	var updateData = function(resource, secondsAgo, callback) {

	    // Fetch data from our data provider
	    provider.getData(resource, secondsAgo, function(newData) {

	      // Store the data locally
			dataAll.resetData();
	    	dataAll.addNewData(newData);

	    	updateNumBars(numUsers, chart);
	        if (callback) {
		       callback();
	        }	      	  
	    });	 	    
	};

	var updateNumBars = function (num, paramChart){

		paramChart.options.data=[];
		for (var i=1; i<num+1; i++) {
			if(i % 2 ==0){
				paramChart.options.data.push(
					{
						type: "column",	
						showInLegend: true,
						name: "user"+i,
						color: "Blue",
						yValueFormatString: "0.##",
						indexLabel: "{y}",
						click: dataClick,
						dataPoints: [
							{ label: "engagement", y: {} },
							{ label: "focus", y: {} },
							{ label: "excitement", y: {} },
							{ label: "frustration", y: {} },
							{ label: "stress", y: {} },
							{ label: "relaxation", y: {} }
						]					
					}			
				);
			}else{
				paramChart.options.data.push(
					{
						type: "column",	
						showInLegend: true,
						name: "user"+i,
						color: "Green",
						yValueFormatString: "0.##",
						indexLabel: "{y}",
						click: dataClick,
						dataPoints: [
							{ label: "engagement", y: {} },
							{ label: "focus", y: {} },
							{ label: "excitement", y: {} },
							{ label: "frustration", y: {} },
							{ label: "stress", y: {} },
							{ label: "relaxation", y: {} }
						]					
					}			
				);
			}	
		};		
	};
	
	var updateChart = function (paramChart , paramData) {
		var barColor, name;
		var dps = new Array(numUsers);		
		var measurements = ["engagement","focus","excitement","frustration","stress","relaxation"];		
		
		for (var i=0; i< numUsers; i++){
			
			dps[i] = paramChart.options.data[i].dataPoints;
			
			name = paramChart.options.data[i].name;
			
	        for (var j=0; j< numUsers; j++){
	        	
				if (paramData[j].name == name) {
					dps[i] = paramData[j].userData;
				}
	        }	
			
	        for (var k= 0; k< dps[i].length; k++){	        	
	        	
				if (dps[i][k].y <= 0.0) {
					dps[i][k].y = 0.01;
				}
				if (dps[i][k].y >= 1.0) {
					dps[i][k].y = 0.99;
				}
				
//				barColor = yVal > 0.8 ? "Red" : yVal >= 0.5 ? "Yellow" : yVal < 0.5 ? "Green" : null;
		        if (dps[i][k].label=="engagement"){
		        	barColor = dps[i][k].y <0.85? "Red": paramChart.options.data[i].color;
		        }
		        else if (dps[i][k].label=="focus"){
		        	barColor = dps[i][k].y <0.75? "Red": paramChart.options.data[i].color;
		        }
		        else if (dps[i][k].label=="excitement"){
		        	barColor = dps[i][k].y <0.65? "Red": paramChart.options.data[i].color;
		        }
		        else if (dps[i][k].label=="frustration"){
		        	barColor = dps[i][k].y >0.25? "Red": paramChart.options.data[i].color;
		        }
		        else if (dps[i][k].label=="stress"){
		        	barColor = dps[i][k].y >0.15? "Red": paramChart.options.data[i].color;
		        }
		        else {
		        	barColor = dps[i][k].y < 0.45 ? "Red": paramChart.options.data[i].color;
		        }
				dps[i][k] = {label: dps[i][k].label , y: dps[i][k].y, color: barColor};
			}
	        chart.options.data[i].dataPoints = dps[i];
		}
		paramChart.render();

	};
	
	
	/**
	 * Provides access to records data.
	 */
	var MeasurementDataProvider = function() {

	  var _endpoint = "http://" + location.host + "/api/GetMeasurements";
	
	  /**
	   * Builds URL to fetch the number of records for a given resource in the past
	   */
	  buildUrl = function(resource, range_in_seconds) {
	    return _endpoint + "?resource=" + resource + "&range_in_seconds="
	        + range_in_seconds;
	  };
	
	  return {
	    /**
	     * Set the endpoint to request records with.
	     */
	    setEndpoint : function(endpoint) {
	      _endpoint = endpoint;
	    },
	
	    /**
	     * Requests new data and passed it to the callback provided. 
	     */
	    getData : function(resource,range_in_seconds,callback) {
	      $.ajax({
	        url : buildUrl(resource, range_in_seconds)
	      }).done(callback);
	    }
	  }
	};
	
	/**
	 * Internal representation of data. 
	 */
	var MeasurementData = function() {

	  var dataPerUser={name:{}, resource:{}, timestamp:{}, userData:[]};
	
	  return {
	    /**
	     * @returns {object} The internal representation of record data.
	     */
	    getData : function() {
	      return data;
	    },
	
	    resetData: function() {
	    	data=[];
	    },
	    /**
	     * Merges new data in to our existing data set.
	     *
	     * @param {object} Record data returned by our data provider.
	     */
	    addNewData : function(newMeasurementData) {
			
			var userSet = new Set();
			
	    	newMeasurementData.forEach(function(record) {
	    		
	    		userSet.add(record.host);
	    		
	    		dataPerUser={name:{}, resource:{}, timeStamp:{}, userData:[]};
	        	dataPerUser.timeStamp = record.timeStamp;
	        	dataPerUser.resource = record.resource;
	        	dataPerUser.name = record.host;
	        	
	    		// Add individual measurement
		        record.values.forEach(function(measurementValue) {
		          // create a new data series entry for this measurement
		        	measureData = 
			          {
			            label : measurementValue.measurement,
			                y : measurementValue.value
			          };
		          
		          // Update the measurement data	
		            dataPerUser.userData.push(measureData);
		        });
		        
		        data.push(dataPerUser);	
		        
	      });   
	    	numUsers= userSet.size;   	
	    },
	    
	    removeDataOlderThan : function(currentTimeStamp) {
			console.log("I am here 5.2");

	        // For each measurement
	          $.each(data, function(measurementData) {
	        	  	          
		            // If the data point is older than the provided time
		            if (measurementData.timestamp < currentTimeStamp) {
		              // Remove the timestamp from the data        	  
		              delete measurementData;
		            }
		       });	     
	     }   
	  }
	};
	
	// event handlers
	function legendClick(e)
	{
	  window.location = 'graph.html?user='+ e.dataSeries.name;  
	}

	function dataClick(e) {
		window.location = 'graph.html?user='+ e.dataSeries.name;  

	}

	var dataAll = new MeasurementData();
	var provider = new MeasurementDataProvider();
	
	// use callbacks to avoid race conditions	
	updateData(resource, secondsAgo, function(){
		updateChart(chart, data);
    });	
	setInterval(function() {
		updateData(resource, secondsAgo, function(){
			updateChart(chart, data);
	    });
	}, 1000);

}
