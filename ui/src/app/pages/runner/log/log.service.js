(function () {
  'use strict';

  angular.module('VampRunner.pages.runner')
    .service('log', ["$rootScope", "$runner", "toastr", function ($rootScope, $runner, toastr) {
      return new Log($rootScope, $runner, toastr);
    }]);

  function Log($rootScope, $runner, toastr) {

    var logs = this.logs = [];

    var push = function (level, source, message) {
      var log = {
        level: level,
        source: source,
        message: message,
        timestamp: Date.now()
      };
      logs.unshift(log);
      while (logs.length > 100) logs.pop();

      if (level === 'info')
        toastr.success(message, level.toUpperCase());
      else if (level === 'error')
        toastr.error(message, level.toUpperCase());

      $rootScope.$broadcast('log', log);
    };

    $rootScope.$on('recipes:run', function () {
      var selected = [];
      for (var i = 0; i < $runner.recipes.length; i++) {
        var recipe = $runner.recipes[i];
        if (recipe.selected) selected.push('\'' + recipe.name + '\'');
      }
      push('info', 'user', 'Run recipes: ' + selected.join(', ') + '.');
    });

    $rootScope.$on('recipe:run', function (event, data) {
      push('info', 'user', 'Run recipe step: [' + data.recipe.name + ' / ' + data.step.description + ']');
    });

    $rootScope.$on('recipe:cleanup', function (event, recipe) {
      push('info', 'user', 'Cleaning up recipe: ' + recipe.name);
    });

    $rootScope.$on('recipe:state', function (event, recipe) {
      if (recipe.state === 'failed')
        push('error', 'system', 'Running recipe failed: \'' + recipe.name + '\'.');
      else if (recipe.state === 'succeeded')
        push('info', 'system', 'Running recipe succeeded: \'' + recipe.name + '\'.');
    });

    $rootScope.$on('vamp:busy', function () {
      push('error', 'system', 'Already busy with "run" or "cleanup" action.');
    });
  }

})();