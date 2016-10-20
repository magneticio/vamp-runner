(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .controller('StepsCtrl', StepsCtrl);

  /** @ngInject */
  function StepsCtrl($scope, $uibModal, baConfig, $runner) {

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

    $scope.run = function (recipe, step) {
      $runner.run(recipe, step);
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

    $scope.open = function (step) {

      $scope.step = step;

      $uibModal.open({
        animation: true,
        templateUrl: 'app/pages/recipes/steps/modal.html',
        size: 'lg',
        scope: $scope
      });
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
