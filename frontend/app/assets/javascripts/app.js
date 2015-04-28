var myApp = angular.module('myApp', [ 'ngRoute' ]);

myApp.controller('MyController', [
    '$scope',
    function($scope) {
      $scope.greeting = 'it works';
      $scope.currencies = [];
      
      // put code for highchart here
      var myChart; // global variable to access chart

      $(function () {

        Highcharts.setOptions({
          global : {
            useUTC : false
          }
        });

        // Create the chart
//        $('#container').highcharts('StockChart', {
        myChart = new Highcharts.StockChart({
          chart : {
            renderTo : 'container'//,
//            events : {
//              load : function () {
      //
//                // set up the updating of the chart each second
//                var series = this.series[0];
//                setInterval(function () {
//                  var x = (new Date()).getTime(), // current time
//                    y = Math.round(Math.random() * 100);
//                  series.addPoint([x, y], true, true);
//                }, 1000);
//              }
//            }
          },

          rangeSelector: {
            buttons: [{
              count: 1,
              type: 'minute',
              text: '1M'
            }, {
              count: 5,
              type: 'minute',
              text: '5M'
            }, {
              type: 'all',
              text: 'All'
            }],
            inputEnabled: false,
            selected: 0
          },

          title : {
            text : 'Live stock data'
          },

          exporting: {
            enabled: false
          },

          series : (function () {
            // generate 10 series with name "i" and empty data
            var seriesTmp = [];

            for (var i = 0; i < 10; ++i) {
              seriesTmp.push({name : i.toString(), data : [0, 0]});
            }
            return seriesTmp;
          }()) 
        });
        
//        $('#button').click(function () {
//          var chart = $('#container').highcharts(),
//            series = chart.series[0];
//          if (series.visible) {
//            series.hide();
//          } else {
//            series.show();
//          }
//        });

      });

      function toggleGraph(id) {
//        console.log(this);
        var button_id = id.split("_")[1];
        series = myChart.series[button_id];
        if (series.visible) {
          series.hide();
        } else {
          series.show();
        }
      }

      var ws = new WebSocket("ws://localhost:9000/wsTest");
      ws.onmessage = function(event) {
//        console.log("WS received TrueFx quotes:");
        
        var quotes = JSON.parse(event.data).quotes;
//        console.log(dataParsed);
        
        var fill = false;
//        // initialize symbol names
        if ($scope.currencies.length < 1) {
          fill = true;
        }

        var x = (new Date()).getTime(); // current time for debugging (fetcher always returns same time step)
        console.log(quotes.length);
        for (var i = 0; i < quotes.length; /*i += 2*/ ++i) {
          var whatC = quotes[i].whatC;
          var withC = quotes[i].withC;
          var y = quotes[i].ask;
//          x = quotes[i].timestamp; // makes the whole graph 
          var series = myChart.series[i];
          series.name = whatC + " to " + withC;
          if (fill)
            {
            $scope.currencies.push(whatC + " to " + withC);
            console.log($scope.currencies);
            }
          var shift = series.data.length > 20 ? true : false;
//          console.log(series.data.length);
          series.addPoint([x, y], false, shift); // bool1: redraw, bool2: shift  
        }

        myChart.redraw();
        
//        console.log($scope.currencies);
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
    } ]);