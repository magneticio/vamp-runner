(function () {
  'use strict';

  angular.module('VampRunner.pages.log')
    .service('log', ["$rootScope", "api", function ($rootScope, api) {
      return new Log($rootScope, api);
    }]);

  function Log($rootScope, api) {

    var entries = this.entries = [];

    var push = function (level, source, message) {
      var log = {
        level: level,
        source: source,
        message: message,
        timestamp: Date.now()
      };
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
      push('info', 'user', 'Run recipes: ' + selected.join(', ') + '.');
    });

    $rootScope.$on('recipes:stop', function () {
      push('info', 'user', 'Running recipes has been stopped.');
    });

    $rootScope.$on('recipes:purge', function () {
      push('info', 'user', 'Purging all artifacts.');
    });

    $rootScope.$on('recipes:success', function (event, recipe) {
      push('info', 'system', 'Running recipe succeeded: \'' + recipe.title + '\'.');
    });

    $rootScope.$on('recipes:failure', function (event, recipe) {
      push('error', 'system', 'Running recipe failed: \'' + recipe.title + '\'.');
    });
  }

})();