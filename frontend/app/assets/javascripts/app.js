var myApp = angular.module('myApp', [ 'ngRoute', 'highcharts-ng' ]);

myApp.controller('MyController', [
		'$scope',
		function($scope) {
			$scope.greeting = 'it works';

			var ws = new WebSocket("ws://localhost:9000/wsTest");
			ws.onmessage = function(event) {
				data = JSON.parse(event.data).quotes;
				$scope.chartSeries
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

			$scope.chartSeries = [];

			$scope.addPoints = function() {
				var seriesArray = $scope.chartConfig.series;
				var rndIdx = Math.floor(Math.random() * seriesArray.length);
				seriesArray[rndIdx].data = seriesArray[rndIdx].data.concat([ 1,
						10, 20 ]);
			};

			$scope.chartConfig = {
				options : {
					chart : {
						type : 'areaspline'
					},
					plotOptions : {
						series : {
							stacking : ''
						}
					}
				},
				series : $scope.chartSeries,
				title : {
					text : 'Hello'
				},
				useHighStocks : true,
				loading : false,
				size : {}
			};

			$scope.reflow = function() {
				$scope.$broadcast('highchartsng.reflow');
			};

		} ]);