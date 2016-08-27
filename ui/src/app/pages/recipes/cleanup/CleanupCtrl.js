(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .controller('CleanupCtrl', CleanupCtrl);

  /** @ngInject */
  function CleanupCtrl($rootScope, $scope, $uibModal, baConfig, api) {

    $scope.transparent = baConfig.theme.blur;

    $scope.recipe = api.recipes[0];

    $scope.isRunning = function () {
      for (var i = 0; i < api.recipes.length; i++) {
        if (api.recipes[i].state === 'running') {
          return true;
        }
      }
      return false;
    };

    $scope.cleanup = function (recipe) {
      api.cleanup(recipe);
    };

    $rootScope.$on('recipe:details', function (event, recipe) {
      $scope.recipe = recipe;
    });

    $rootScope.$on('recipes:update', function () {
      for (var i = 0; i < api.recipes.length; i++) {
        var recipe = api.recipes[i];
        if (recipe.state === 'running') {
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
