(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .controller('StepsCtrl', StepsCtrl);

  /** @ngInject */
  function StepsCtrl($rootScope, $scope, baConfig, api) {

    $scope.transparent = baConfig.theme.blur;

    $scope.recipe = api.recipes[0];

    var follow = false;

    $scope.isRunning = function () {
      for (var i = 0; i < api.recipes.length; i++) {
        if (api.recipes[i].state === 'running') {
          return true;
        }
      }
      return false;
    };

    $scope.run = function (recipe, step) {
      api.run(recipe, step);
    };

    $scope.isRunnable = function (recipe, step) {
      if (step.dirty) return true;

      var runnable = true;

      for (var i = 0; i < recipe.run.length; i++) {
        var s = recipe.run[i];
        if (s.id == step.id)
          return runnable;
        else {
          runnable = runnable && s.dirty;
          if (!runnable) return false;
        }
      }

      return false;
    };

    $rootScope.$on('recipe:details', function (event, recipe) {
      $scope.recipe = recipe;
      follow = false;
    });

    $rootScope.$on('recipes:run', function () {
      follow = true;
    });

    $rootScope.$on('recipes:update', function () {
      for (var i = 0; i < api.recipes.length; i++) {
        var recipe = api.recipes[i];
        if (recipe.state === 'running' && follow) {
          $scope.recipe = recipe;
          return;
        } else {
          if ($scope.recipe && $scope.recipe.id == recipe.id) $scope.recipe = recipe;
        }
      }

      if (!$scope.recipe) $scope.recipe = api.recipes[0];
    });
  }
})();
