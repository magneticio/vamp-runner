(function () {
  'use strict';

  angular.module('VampRunner.api')
    .service('api', ["$rootScope", "$websocket", "$timeout", function ($rootScope, $websocket, $timeout) {
      return new Api($rootScope, $websocket, $timeout);
    }]);

  function Api($rootScope, $websocket, $timeout) {

    var info = this.info = {};
    var config = this.config = {};
    var loads = this.loads = [];
    var recipes = this.recipes = [];

    var dataStream;

    var command = function (cmd, args, emit, event) {
      if (!dataStream) return;

      dataStream.send({
        command: cmd,
        arguments: args
      });
      $rootScope.$emit(emit, event);
    };

    var process = function (message) {

      var data = JSON.parse(message);

      if (data['type'] === 'info') {

        for (var key in data) {
          if (data.hasOwnProperty(key)) {
            info[key] = data[key];
          }
        }

        $rootScope.$emit('vamp:info', info);

      } else if (data['type'] === 'config') {

        for (var key in data) {
          if (data.hasOwnProperty(key)) {
            config[key] = data[key];
          }
        }

        $rootScope.$emit('vamp:config', config);

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

          for (var j = 0; j < recipe["steps"].length; j++) {
            var step = recipe["steps"][j];
            step.state = step.state.toLowerCase();

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

          //

          if (select) {
            recipe.selected = true;
          } else {
            for (var k = 0; k < old.length; k++) {
              if (old[k].id === recipe['id']) {
                recipe.selected = old[k].selected;
                if (old[k].state !== recipe.state) $rootScope.$emit('recipe:state', recipe);
                break;
              }
            }
          }

          recipes.push(recipe);
        }

        $rootScope.$emit('recipes:update', '');
      }
    };

    var selected = function () {
      var result = [];

      for (var i = 0; i < recipes.length; i++) {
        var recipe = recipes[i];
        if (recipe.selected) result.push(recipe.id);
      }

      return result;
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
      } else {
        var recipes = selected();
        if (recipes.length > 0) command('run', recipes, 'recipes:run');
      }
    };

    this.cleanup = function () {
      var recipes = selected();
      if (recipes.length > 0) command('cleanup', recipes, 'recipes:cleanup');
    };

    this.init = function () {

      var channel = function () {

        dataStream = $websocket('ws://localhost:8088/channel');

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