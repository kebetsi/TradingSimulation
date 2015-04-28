var myApp = angular.module('myApp', [ 'ngRoute', 'highcharts-ng' ]);

myApp.controller('MyController', [ '$scope', function($scope) {
  $scope.greeting = 'it works';
  $scope.currencies = [];

  // put code for highchart here
  var myChart; // global variable to access chart

  $(function() {

    Highcharts.setOptions({
      global : {
        useUTC : false
      }
    });

    // Create the chart
    myChart = new Highcharts.StockChart({
      chart : {
        renderTo : 'container'
      },

      rangeSelector : {
        buttons : [ {
          count : 1,
          type : 'minute',
          text : '1M'
        }, {
          count : 5,
          type : 'minute',
          text : '5M'
        }, {
          type : 'all',
          text : 'All'
        } ],
        inputEnabled : false,
        selected : 0
      },

      title : {
        text : 'Live stock data'
      },

      exporting : {
        enabled : false
      },

      series : (function() {
        // generate 10 series with name "i" and empty data
        var seriesTmp = [];

        for (var i = 0; i < 10; ++i) {
          seriesTmp.push({
            name : i.toString(),
            data : [ 0, 0 ]
          });
        }
        return seriesTmp;
      }())
    });

    // $('#button').click(function () {
    // var chart = $('#container').highcharts(),
    // series = chart.series[0];
    // if (series.visible) {
    // series.hide();
    // } else {
    // series.show();
    // }
    // });

  });

  function toggleGraph(id) {
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
    var quotes = JSON.parse(event.data).quotes;
    var fill = false;
    // initialize symbol names
    if ($scope.currencies.length < 10) {
      fill = true;
    }

    var x = quotes[0].timestamp;  // note: timestamps for different symbols are not necessarily equal
                                  // taking only one timestamp makes the graph faster and smoother for the cost of accuracy
    for (var i = 0; i < quotes.length; /* i += 2 */++i) {
      var whatC = quotes[i].whatC;
      var withC = quotes[i].withC;
      var y = quotes[i].ask;
      var series = myChart.series[i];
      if (fill) {
        series.name = whatC + " to " + withC;
        $scope.currencies.push(whatC + " to " + withC);
      }
      var shift = (series.data.length > 20);
      series.addPoint([ x, y ], false, shift); // bool1: redraw, bool2: shift
    }

    myChart.redraw();
    $scope.$apply(); // this makes sure that the bound variables are updated
                      // even though we are in an asynchronous call
    // http://www.jeffryhouser.com/index.cfm/2014/6/2/How-do-I-run-code-when-a-variable-changes-with-AngularJS
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

// table controller
myApp.controller('TabController', [ '$scope', function($scope) {
  $scope.tab = 1;

  $scope.selectTab = function(setTab) {
    $scope.tab = setTab;
  };
  $scope.isSelected = function(checkTab) {
    return $scope.tab === checkTab;
  };
  
  // traders
  $scope.traders = [
    {
      name : "joe",
      strategy : "random",
      initMoney : 10,
      currentMoney : 100,
      transactions : 5,
      earnings : 90 
    },
    {
      name : "joe",
      strategy : "random",
      initMoney : 10,
      currentMoney : 100,
      transactions : 5,
      earnings : 90 
    }
    ];
  
  $scope.transactions = [
    {
      seller : 'a',
      buyer : 'b',
      price : 10,
      amount : 100,
      time : '10:14'
    },
    {
      seller : 'a',
      buyer : 'b',
      price : 10,
      amount : 100,
      time : '10:14'
    }
    ];
  
  $scope.wallet = [
     {
       name : 'eur',
       amount : 100
     },
     {
       name : 'chf',
       amount : 10
     }
     ];
} ]);
