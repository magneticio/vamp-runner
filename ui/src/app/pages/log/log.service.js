(function () {
  'use strict';

  angular.module('VampRunner.pages.log')
    .service('log', ["$rootScope", "api", function ($rootScope, api) {
      return new Log($rootScope, api);
    }]);

  function Log($rootScope, api) {

    var entries = this.entries = [];

    var push = function(log) {
      log.timestamp = Date.now();
      entries.unshift(log);
      while (entries.length > 100) entries.pop();
      $rootScope.$emit('log', log);
    };

    $rootScope.$on('recipes:run', function () {
      var selected = [];

      for (var i = 0; i < api.recipes.length; i++) {
        var recipe = api.recipes[i];
        if (recipe.selected) selected.push('\'' + recipe.title + '\'');
      }

      push({
        level: 'info',
        source: 'user',
        message: 'Run recipes: ' + selected.join(', ') + '.'
      });
    });

    $rootScope.$on('recipes:stop', function () {
      push({
        level: 'info',
        source: 'user',
        message: 'Running recipes has been stopped.'
      });
    });

    $rootScope.$on('recipes:success', function (event, recipe) {
      push({
        level: 'info',
        source: 'system',
        message: 'Running recipe succeeded: \'' + recipe.title + '\'.'
      });
    });

    $rootScope.$on('recipes:failure', function (event, recipe) {
      push({
        level: 'error',
        source: 'system',
        message: 'Running recipe failed: \'' + recipe.title + '\'.'
      });
    });
  }

})();