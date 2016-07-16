(function () {
  'use strict';

  angular.module('VampRunner.api')
    .service('api', ["$rootScope", "$interval", "$websocket", "$timeout", function ($rootScope, $interval, $websocket, $timeout) {
      return new Api($rootScope, $interval, $websocket, $timeout);
    }]);

  function Api($rootScope, $interval, $websocket, $timeout) {

    var info = this.info = {};
    var loads = this.loads = [];

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

      $rootScope.$emit('recipes:run', recipes);

      var index = 0;

      for (var i = index; i < recipes.length; i++) {
        if (recipes[i].selected) recipes[i].state = 'idle';
      }

      function step() {
        for (var i = index; i < recipes.length; i++) {
          index = i + 1;
          var recipe = recipes[i];
          if (recipe.selected) {
            if (recipe.state === 'running') {
              recipe.state = (Math.random() > 0.5 ? 'success' : 'failure');
              $rootScope.$emit('recipes:' + recipe.state, recipe);
            } else {
              recipes[i].state = 'running';
              $rootScope.$emit('recipes:running', recipe);
              index = i;
              break;
            }
          }
        }
        $rootScope.$emit('recipes:update', '');
      }

      runner = $interval(function () {
        if (index === recipes.length) $interval.cancel(runner); else step();
      }, 2000);

      step();
    };

    this.stop = function () {

      $rootScope.$emit('recipes:stop', '');

      $interval.cancel(runner);
      for (var i = 0; i < recipes.length; i++) {
        var recipe = recipes[i];
        if (recipe.state === 'running') {
          recipe.state = (Math.random() > 0.5 ? 'success' : 'failure');
          $rootScope.$emit('recipes:' + recipe.state, recipe);
        }
      }
      $rootScope.$emit('recipes:update', '');
    };

    // start

    var process = function (message) {
      console.log(message);

      var data = JSON.parse(message);

      if (data['type'] === 'info') {

        for (var key in data) {
          if (data.hasOwnProperty(key)) {
            info[key] = data[key];
          }
        }

        $rootScope.$emit('vamp:info', info);

      } else if (data['type'] === 'load') {

        var load = {
          cpu: data['cpu'],
          heap: {
            max: data['heap']['max'] / (1024 * 1024),
            used: data['heap']['used'] / (1024 * 1024)
          }
        };
        load.heap.percentage = 100 * load.heap.used / load.heap.max;

        loads.push(load);
        while (loads.length > 100) loads.shift();

        $rootScope.$emit('vamp:load', load);
      }
    };

    this.init = function () {

      var channel = function () {
        var dataStream = $websocket('ws://localhost:8080/channel');

        dataStream.onOpen(function () {
          dataStream.send("info")
        });

        dataStream.onClose(function () {
          console.log("closed, will try to reconnect in 5 seconds...");
          $timeout(channel, 5000);
        });

        dataStream.onMessage(function (message) {
          process(message.data);
        });
      };

      channel();
    };
  }

})();