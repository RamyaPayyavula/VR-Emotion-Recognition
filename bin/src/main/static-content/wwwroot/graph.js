
// When the page loads create our graph and start updating it.
$(function() {
	getUser("user");
//	alert("user passed to this page is: "+ userTosplay);
  graph.inject();
  uiHelper.decorate();
  uiHelper.start();
});

function getUser(user)
{
 var query = window.location.search.substring(1);
 var vars = query.split("&");
 for (var i=0;i<vars.length;i++) {
         var pair = vars[i].split("=");
         if(pair[0] == user){            	   
        	userToDisplay = pair[1];}
 }       
 return(false);
}

/**
 * Represents a Flot time series graph that is capable of updating itself with
 * new data.
 */
var Graph = function() {

  var graph, totalDurationToGraphInSeconds = 120;

  return {
    /**
     * @returns {number} the total duration of time, in seconds, this graph will display.
     */
    getTotalDurationToGraphInSeconds : function() {
      return totalDurationToGraphInSeconds;
    },

    /**
     * Creates the graph and injects in into the element with id="graph".
     */
    inject : function() {
      graph = $.plot("#graph", {},
          {
            // Define the colors and y-axis margins for the graph.
            grid : {
              borderWidth : 1,
              minBorderMargin : 20,
              labelMargin : 10,
              backgroundColor : {
                colors : [ "#fff", "#fff5e6" ]
              },
              margin : {
                top : 8,
                bottom : 20,
                left : 20
              },
            },
            // Do not render shadows for our series lines. This just slows us
            // down.
            series : {
              shadowSize : 0
            },
            // Set up the y-axis to initially show 0-10. This is dynamically
            // adjusted as data is updated.
            yaxis : {
              min : 0,
              max : 10
            },
            // The x-axis is time-based. The local browser's timezone will be
            // used to interpret timestamps. The range is dynamically adjusted
            // as data is updated.
            xaxis : {
              mode : "time",
              timezone : "browser",
              timeformat : "%M:%S",
              min : (new Date()).getTime()
                  - (totalDurationToGraphInSeconds * 1000),
              max : (new Date()).getTime()
            },
            // Show the legend of unique referrers in the upper-right corner of
            // the graph.
            legend : {
              show : true,
              position : "nw"
            }
          });

      // Create y-axis label and inject it into the graph container
      var yaxisLabel = $("<div class='axisLabel yaxisLabel'></div>").text(
          "Measurements received from EEG sensor every second").appendTo("#graph");
      // Center the y-axis along the left side of the graph
      yaxisLabel.css("margin-top", yaxisLabel.width() / 2 - 20);
    },

    /**
     * Update the graph to use the data provided. This completely replaces any
     * existing data and recomputes the axes' range.
     *
     * @param {Object}
     *          flotData Flot formatted data object that should include at a
     *          minimum series labels and their data in the format: { label: "my
     *          series", data: [[0, 10], [1, 100]] }
     */
    update : function(flotData) {
      graph.setData(flotData);
      
      // Calculate min and max value to update y-axis range.
      var getValue = function(tuple) {
        // Flot data values are stored as the second element of each data array
        return tuple[1];
      };
      var max = Number.MIN_VALUE;
      flotData.forEach(function(d) {
        m = Math.max.apply(Math, d.data.map(getValue));
        max = Math.max(m, max);
      });
      var min = Number.MAX_VALUE;
      flotData.forEach(function(d) {
        m = Math.min.apply(Math, d.data.map(getValue));
        min = Math.min(m, min);
      });

      // Adjust the y-axis for min/max of our new data
      graph.getOptions().yaxes[0].max = Math.min(max, max)
      graph.getOptions().yaxes[0].min = min

      // Adjust the x-axis to move in real time and show at most the total
      // duration to graph as configured above
      graph.getOptions().xaxes[0].min = (new Date()).getTime()
          - (totalDurationToGraphInSeconds * 1000),
          graph.getOptions().xaxes[0].max = (new Date()).getTime()

      // Redraw the graph data and axes
      graph.draw();
      graph.setupGrid();
    }
  }
}

/**
 * A collection of methods used to manipulate visible elements of the page.
 */
var UIHelper = function(data, graph) {
  // How frequently should we poll for new data and update the graph?
  var updateIntervalInMillis = 1000;
  // How often should the average be updated?
  var intervalsPerAverage = 5;
  // How far back should we fetch data at every interval?
  var rangeOfDataToFetchEveryIntervalInSeconds = 120;
  // how many measurements does average display?
  var numMeasurementToDisplay = 6;
  // Keep track of when we last updated the average.
  var averageIntervalCounter = 1;
  // Controls the update loop.
  var running = true;
  // Set the active resource to query for when updating data.
  var activeResource = "EEG sensor";

  /**
   * Fetch records from the last secondsAgo seconds.
   *
   * @param {string}
   *          resource The resource to fetch records for.
   * @param {number}
   *          secondsAgo The range in seconds since now to fetch records for.
   * @param {function}
   *          callback The callback to invoke when data has been updated.
   */
  var updateData = function(resource, secondsAgo, callback) {
    // Fetch data from our data provider
    provider.getData(resource, secondsAgo, function(newData) {
      // Store the data locally
      data.addNewData(newData);
      
      // Remove data that's outside the window of data we are displaying. This
      // is unnecessary to keep around.
      data.removeDataOlderThan((new Date()).getTime()
          - (graph.getTotalDurationToGraphInSeconds() * 1000));
      if (callback) {
        callback();
      }
    });
  }

  /**
   * Update the average display.
   */
  var updateAverage = function() {
    var average = data.getAverage(numMeasurementToDisplay);

    var table = $("<table/>").addClass("measurement");
    $.each(average, function(_, v) {
      console.loog
      var row = $("<tr/>");
      row.append($("<td/>").addClass('measurementColumn').text(v.measurement));
      row.append($("<td/>").addClass('valueColumn').text(v.value));
      table.append(row);
    });

    $("#measurement").html(table);
  }

  /**
   * Update the graph with new data.
   */
  var update = function() {
    // Update our local data for the active resource
    updateData(activeResource, rangeOfDataToFetchEveryIntervalInSeconds);

    // Update average every intervalsPerAverage intervals
    if (averageIntervalCounter++ % intervalsPerAverage == 0) {
      updateAverage(data);
      averageIntervalCounter = 1;
    }
       
    // Update the graph with our new data, transformed into the data series
    // format Flot expects
    graph.update(data.toFlotData());
    
    // Update the user displayed
    setUserDisplayed(userToDisplay);
    // If we're still running schedule this method to be executed again at the
    // next interval
    if (running) {
      setTimeout(arguments.callee, updateIntervalInMillis);
    }
  }

  /**
   * Set the page description header.
   *
   * @param {string}
   *          desc Page description.
   */
  var setDescription = function(desc) {
    $("#description").text(desc);
  }

  /**
   * Set the user displayed
   *
   * @param {string}
   *          s The new host that last updated record data. If one is not
   *          provided the last updated label will not be shown.
   */
  var setUserDisplayed = function(s) {
    var message = s ? s : "";
    $("#userDisplayed").text(message);
//    alert("user displayed is "+ s);
  }

  return {
    /**
     * Set the active resource the graph is displaying records for. This is for
     * debugging purposes.
     *
     * @param {string}
     *          resource The resource to query our data provider for records of.
     */
    setActiveResource : function(resource) {
      activeResource = resource;
      data.removeDataOlderThan((new Date()).getTime());
    },

    /**
     * Decorate the page. This will update various UI elements with dynamically
     * calculated values.
     */
    decorate : function() {
      setDescription("This graph displays the last "
          + graph.getTotalDurationToGraphInSeconds()
          + " seconds of EEG sensor measurement records for: \n"+ userToDisplay);
      
      $("#measurementDescription").text("Average values of all " + numMeasurementToDisplay + " measurements in the recent "
              + (intervalsPerAverage * updateIntervalInMillis) + " ms: ");
      
    },

    /**
     * Starts updating the graph at our defined interval.
     */
    start : function() {
      setDescription("Loading data...");
      var _this = this;
      // Load an initial range of data, decorate the page, and start the update polling process.
      updateData(activeResource, rangeOfDataToFetchEveryIntervalInSeconds,
          function() {
            // Decorate again now that we're done with the initial load
            _this.decorate();
            // Start our polling update
            running = true;
            update();
          });
    },

    /**
     * Stop updating the graph.
     */
    stop : function() {
      running = false;
    }
  }
};

/**
 * Provides easy access to records data.
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
    getData : function(resource, range_in_seconds, callback) {
      $.ajax({
        url : buildUrl(resource, range_in_seconds)
      }).done(callback);
    }
  }
}

/**
 * Internal representation of data. 
 */
var MeasurementData = function() {

  var data = {};
  var totals = {};
  var counts = {};
  var averages = {};
  

  /**
   * Update the average for each measurement.
   */
  var updateAverage = function(measurement) {
    // Simply loop through all the records and sum them
    if (data[measurement]) {
      totals[measurement] = 0;
      counts[measurement] = 0;
      averages[measurement] = 0;
      $.each(data[measurement].data, function(ts, value) {
        totals[measurement] += value; 
        counts[measurement] ++;
      });
      averages[measurement] = totals[measurement]/counts[measurement];

    } else {
    }
  }

  return {
    /**
     * @returns {object} The internal representation of record data.
     */
    getData : function() {
      return data;
    },

    /**
     * @returns {object} An associative array of measurements to averages.
     */
    averagesAsArray : function() {
      return averages;
    },

    /**
     * Compute average using the data we have.
     *
     * @param {number}
     *          n The number of measurement to calculate.
     *
     * @returns {object[]} The measurement averages in descending order.
     */
    getAverage : function(n) {
      // Create an array out of the averages
      var averagesAsArray = $.map(averages, function(value, measurement) {
        return {
          'measurement' : measurement,
          'value' : value
        };
      });
      return averagesAsArray;
    },

    /**
     * Merges new data in to our existing data set.
     *
     * @param {object} Record data returned by our data provider.
     */
    addNewData : function(newMeasurementData) {

    	newMeasurementData.forEach(function(record) {
    		if(record.host==userToDisplay){
	        // Update the host who last updated the record
	        record.values.forEach(function(measurementValue) {
	          // Reuse or create a new data series entry for this measurement
	          measureData = data[measurementValue.measurement] || {
	            label : measurementValue.measurement,
	            data : {}
	          };
	          // Set the measurement value
	          measureData.data[record.timeStamp] = measurementValue.value;
	          
	          // Update the measurement data
	          data[measurementValue.measurement] = measureData;
	          // Update our averages whenever new data is added
	          updateAverage(measurementValue.measurement);
	        });
    	  }
    		else {
//    			continue;
    		}
      });
    },

    /**
     * Removes data older than a specific time.
     *
     * @param {number}
     *          timestamp Any data older than this time will be removed.
     */
    removeDataOlderThan : function(timeStamp) {
      // For each measurement
        $.each(data, function(measurement, measurementData) {
        var shouldUpdateAverages = false;
        // For each data point
        $.each(measurementData.data, function(ts, value) {
          // If the data point is older than the provided time
          if (ts < timeStamp) {
            // Remove the timestamp from the data        	  
            //delete measurementData.data[ts];
        	  
            // We need to update the averages for this measurement since we
            // removed data
            shouldUpdateAverages = true;            
          }
        });
        if (shouldUpdateAverages) {
          // Update the averages if we removed any data
          updateAverage(measurement);
        }
      });
    },

    /**
     * Convert our internal data to a Flot data object.
     */    
    toFlotData : function() {
      flotData = [];
      $.each(data, function(measurement, measureData) {
        flotData.push({
          label : measurement,
          // Flot expects time series data to be in the format:
          // [[timestamp as number, value]]
          data : $.map(measureData.data, function(value, ts) {
          //data : $.map(measureData.data, function(value, ts) {
          return [ [ parseInt(ts), value ] ];
          })
        });
      });
      return flotData;
    }
  }
}

var userToDisplay;
var data = new MeasurementData();
var provider = new MeasurementDataProvider();
var graph = new Graph();
var uiHelper = new UIHelper(data, graph);
