(function() {
  'use strict';

  var app = angular.module('myApp', [ 'ngRoute', 'ui.bootstrap', 'angular-websocket' ]);


  
  app.controller('MyController', [ '$scope', 'alertService',
      function($scope, alertService) {
        $scope.alerts = alertService.get();
        
        $scope.quotes = [];
        
        var ws = new WebSocket('ws://localhost:9000/fetchers/quote');
        
        ws.onmessage = function(event) {
          var quote = JSON.parse(event.data);
          $scope.$apply(function() {
            console.log(quote);
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
        
   } ]);

})();