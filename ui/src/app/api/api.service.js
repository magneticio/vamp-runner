(function () {
  'use strict';

  angular.module('VampRunner.api')
    .service('$runner', ["$rootScope", "$websocket", "$timeout", function ($rootScope, $websocket, $timeout) {
      return new Runner($rootScope, $websocket, $timeout);
    }]);

  function Runner($rootScope, $websocket, $timeout) {

    var info = this.info = {};
    var config = this.config = {};
    var events = this.events = [];
    var loads = this.loads = [];
    var recipes = this.recipes = [];

    var dataStream;

    var command = function (cmd, args, emit, event) {
      if (!dataStream) return;

      dataStream.send({
        command: cmd,
        arguments: args
      });
      $rootScope.$broadcast(emit, event);
    };

    var process = function (message) {

      var data = JSON.parse(message);

      if (data['type'] === 'info') {

        for (var key in data) {
          if (data.hasOwnProperty(key)) {
            info[key] = data[key];
          }
        }

        $rootScope.$broadcast('vamp:info', info);

      } else if (data['type'] === 'config') {

        for (var key in data) {
          if (data.hasOwnProperty(key)) {
            config[key] = data[key];
          }
        }

        $rootScope.$broadcast('vamp:config', config);

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

        $rootScope.$broadcast('vamp:load', load);

      } else if (data['type'] === 'recipes') {

        var select = recipes.length === 0;

        var old = recipes.slice(0);

        recipes.length = 0;

        for (var i = 0; i < data['recipes'].length; i++) {
          var recipe = data['recipes'][i];

          // state processing

          var succeeded = 0;
          var failed = 0;
          var running = 0;
          var idle = 0;

          for (var j = 0; j < recipe["run"].length; j++) {
            var step = recipe["run"][j];
            step.state = step.state;

            if (step.state === 'succeeded')
              succeeded++;
            else if (step.state === 'failed')
              failed++;
            else if (step.state === 'running')
              running++;
            else if (step.state === 'idle')
              idle++;
          }

          if (failed > 0) recipe.state = 'failed';
          else if (running > 0) recipe.state = 'running';
          else if (succeeded > 0) recipe.state = 'succeeded';
          else recipe.state = 'idle';

          recipes.push(recipe);
        }

        $rootScope.$broadcast('recipes:update', '');

      } else if (data['type'] === 'event') {
        var e = {
          timestamp: new Date(),
          tags: data['tags'].join(', '),
          value: data['value']
        };
        events.push(e);
        while (events.length > 100) events.shift();
        $rootScope.$broadcast('vamp:event', e);

      } else if (data['type'] === 'busy') {
        $rootScope.$broadcast('vamp:busy', 'busy');
      } else if (data['type'] === 'vamp-connection-error') {
         $rootScope.$broadcast('vamp:error');
      }
    };

    this.run = function (recipe, step) {
      if (recipe) {
        command('run', {
          recipe: recipe.id,
          step: step.id
        }, 'recipe:run', {
          recipe: recipe,
          step: step
        });
      }
    };

    this.cleanup = function (recipe) {
      command('cleanup', [ recipe.id ], 'recipe:cleanup', recipe);
    };

    this.init = function () {

      var channel = function () {

        dataStream = $websocket('ws://' + window.location.host + '/channel');

        dataStream.onOpen(function () {
          command('info');
          command('config');
          command('recipes');
        });

        dataStream.onClose(function () {
          console.log('closed, will try to reconnect in 5 seconds...');
          dataStream = null;
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