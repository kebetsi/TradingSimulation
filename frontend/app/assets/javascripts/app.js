(function() {
  'use strict';

  var app = angular.module('myApp', [ 'ngRoute', 'ui.bootstrap',
      'highcharts-ng' ]);

  app.controller('MyController', [ '$scope', 'alertService',
      function($scope, alertService) {
        $scope.alerts = alertService.get();

        var ws = new WebSocket('ws://localhost:9000/fetchers/quote');

        ws.onmessage = function(event) {
          var quote = JSON.parse(event.data);
          $scope.$apply(function() {
            var name = quote.whatC.name + " to " + quote.withC.name;

            var quoteSeries = $scope.chartSeries.filter(function(series) {
              return series.name == name;
            })[0];

            if (!quoteSeries) {
              quoteSeries = {
                'name' : name,
                'visible' : false,
                'data' : []
              };

              if ($scope.chartSeries.length === 0) {
                quoteSeries.visible = true;
              }

              $scope.chartSeries.push(quoteSeries);
            }

            quoteSeries.data.push([ quote.timestamp, quote.ask ]);

            if ($scope.chartConfig.loading) {
              $scope.chartConfig.loading = false;
            }

          });
        };

        ws.onclose = function(event) {
          $scope.$apply(function() {
            alertService.add('info', 'Closed connection to the backend');
          });
        };

        ws.onerror = function(event) {
          $scope.$apply(function() {
            alertService.add('danger', 'Lost connection to the backend');
          });
        };

        $scope.chartSeries = [];

        $scope.chartConfig = {
          options : {
            xAxis : [ {
              type : 'datetime'
            } ],
          },
          series : $scope.chartSeries,
          title : {
            text : 'Market price'
          },
          credits : {
            enabled : false
          },
          loading : true,
          size : {}
        };

      } ]);

})();