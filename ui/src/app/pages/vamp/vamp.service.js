(function () {
  'use strict';

  angular.module('VampRunner.pages.vamp')
    .service('vamp', ["$rootScope", "$interval", "$http", function($rootScope, $interval, $http) {
      return new Vamp($rootScope, $interval, $http);
    }]);

  function Vamp($rootScope, $interval, $http) {

    var api = 'http://192.168.99.100:8080/api/v1/info';

    var loads = [];
    this.loads = loads;

    var info = {};
    this.info = info;

    this.start = function () {
      loadPolling();
      infoPolling();
    };

    function loadPolling() {
      function load() {

        $http.get(api + '?for=jvm').success(function(data) {

          var load = {
            cpu: 0,
            heap: {
              max: 0,
              used: 0,
              percentage: 0
            }
          };

          try {
            load = {
              cpu: data.jvm.operating_system.system_load_average,
              heap: {
                max: data.jvm.memory.heap.max / (1024 * 1024),
                used: data.jvm.memory.heap.used / (1024 * 1024)
              }
            };
            load.heap.percentage = 100 * load.heap.used / load.heap.max;
          } catch (e) {}

          loads.push(load);
          while (loads.length > 100) loads.shift();

          $rootScope.$emit("vamp:load", load);
        });
      }

      $interval(load, 3000, 1);
      $interval(load, 10000);
    }

    function infoPolling() {

      function process(data) {
        info["version"] = data.version;
        info["persistence"] = data.persistence.database.type;
        info["key_value_store"] = data.key_value.type;
        info["gateway_driver"] = 'haproxy ' + data.gateway_driver.marshaller.haproxy;
        info["container_driver"] = data.container_driver.type;
        info["workflow_driver"] = '';

        for(var name in data.workflow_driver) {
          info["workflow_driver"] += info["workflow_driver"] == '' ? name : ',' + name;
        }
      }

      var promise = $interval(function () {
        $http.get(api).success(function(data) {
          process(data);
          $rootScope.$emit("vamp:info", data);
          $interval.cancel(promise);
        });
      }, 3000);
    }
  }

})();