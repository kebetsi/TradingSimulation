var myApp = angular.module('myApp', [ 'ngRoute', 'highcharts-ng' ]);

myApp.controller('MyController', [
		'$scope',
		function($scope) {
			$scope.greeting = 'it works';

		} ]);