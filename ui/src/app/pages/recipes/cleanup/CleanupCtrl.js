(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .controller('CleanupCtrl', CleanupCtrl);

  /** @ngInject */
  function CleanupCtrl($scope, baConfig, $runner) {

    $scope.transparent = baConfig.theme.blur;

    $scope.recipe = $runner.recipes[0];

    $scope.isRunning = function () {
      for (var i = 0; i < $runner.recipes.length; i++) {
        if ($runner.recipes[i].state === 'running') {
          return true;
        }
      }
      return false;
    };

    $scope.cleanup = function (recipe) {
      $runner.cleanup(recipe);
    };

    $scope.$on('recipe:details', function (event, recipe) {
      $scope.recipe = recipe;
    });

    $scope.$on('recipes:update', function () {
      for (var i = 0; i < $runner.recipes.length; i++) {
        var recipe = $runner.recipes[i];
        if (recipe.state === 'running') {
          $scope.recipe = recipe;
          return;
        } else {
          if ($scope.recipe && $scope.recipe.id == recipe.id) $scope.recipe = recipe;
        }
      }

      if (!$scope.recipe) $scope.recipe = $runner.recipes[0];
    });
  }
})();
