(function() {
  'use strict';

  angular.module('myApp').factory('alertService', function() {
    var service = {};
    var alerts = [];

    service.get = function() {
      return alerts;
    };

    service.clearAll = function() {
      alerts = [];
    };

    service.add = function(type, msg) {
      return alerts.push({
        type : type,
        msg : msg,
        close : function() {
          return close(this);
        }
      });
    };

    service.close = function(alert) {
      return closeByIndex(alerts.indexOf(alert));
    };

    service.closeByIndex = function(index) {
      return alerts.splice(index, 1);
    };

    return service;
  });

})();