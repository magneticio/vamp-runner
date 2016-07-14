(function () {
  'use strict';

  angular.module('VampRunner.api')
    .service('api', ["$rootScope", "$interval", "$http", function ($rootScope, $interval, $http) {
      return new Api($rootScope, $interval, $http);
    }]);

  function Api($rootScope, $interval, $http) {

    var endpoint = 'http://192.168.99.100:8080/api/v1/info';

    // Vamp info

    var loads = this.loads = [];
    var info = this.info = {};

    function loadPolling() {
      function load() {

        $http.get(endpoint + '?for=jvm').success(function (data) {

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
          } catch (e) {
          }

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

        for (var name in data.workflow_driver) {
          info["workflow_driver"] += info["workflow_driver"] === '' ? name : ',' + name;
        }
      }

      var promise = $interval(function () {
        $http.get(endpoint).success(function (data) {
          process(data);
          $rootScope.$emit("vamp:info", data);
          $interval.cancel(promise);
        });
      }, 3000);
    }

    // recipes

    var recipes = this.recipes = [
      {
        id: "1",
        title: 'HTTP Deployment',
        state: 'idle',
        selected: true
      },
      {
        id: "2",
        title: 'HTTP Canary',
        state: 'idle',
        selected: true
      },
      {
        id: "3",
        title: 'HTTP with Dependencies',
        state: 'idle',
        selected: true
      },
      {
        id: "4",
        title: 'HTTP Flip-Flop Versions',
        state: 'idle',
        selected: true
      },
      {
        id: "5",
        title: 'HTTP Flip-Flop Versions with Dependencies',
        state: 'idle',
        selected: true
      },
      {
        id: "6",
        title: 'TCP Deployment',
        state: 'idle',
        selected: true
      },
      {
        id: "7",
        title: 'TCP with Dependencies',
        state: 'idle',
        selected: true
      },
      {
        id: "8",
        title: 'Route Weights',
        state: 'idle',
        selected: true
      },
      {
        id: "9",
        title: 'Route Weights with Condition Strength',
        state: 'idle',
        selected: true
      },
      {
        id: "10",
        title: 'Scaling In/Out',
        state: 'idle',
        selected: true
      }
    ];

    // running recipes

    var runner;

    this.run = function () {

      var index = 0;

      for (var i = index; i < recipes.length; i++) {
        if (recipes[i].selected) recipes[i].state = 'idle';
      }

      function step() {
        for (var i = index; i < recipes.length; i++) {
          index = i + 1;
          if (recipes[i].selected) {
            if (recipes[i].state === 'running') {
              recipes[i].state = (Math.random() > 0.5 ? 'success' : 'failure');
            } else {
              recipes[i].state = 'running';
              index = i;
              break;
            }
          }
        }
        $rootScope.$emit("recipes:update", recipes);
      }

      runner = $interval(function () {
        if (index === recipes.length) $interval.cancel(runner); else step();
      }, 2000);

      step();
    };

    this.stop = function () {
      $interval.cancel(runner);
      for (var i = 0; i < recipes.length; i++) {
        if (recipes[i].state === 'running') recipes[i].state = (Math.random() > 0.5 ? 'success' : 'failure');
      }
      $rootScope.$emit("recipes:update", recipes);
    };

    // start

    this.init = function () {
      loadPolling();
      infoPolling();
    };
  }

})();