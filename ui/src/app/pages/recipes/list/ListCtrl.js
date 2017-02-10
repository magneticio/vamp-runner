(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .controller('ListCtrl', ListCtrl);

  /** @ngInject */
  function ListCtrl($rootScope, $scope, baConfig, $runner) {
    $scope.transparent = baConfig.theme.blur;
    $scope.recipes = $runner.recipes;

    if ($scope.recipes.length > 0) {
      $scope.recipes[0].active = true;
    }
    $scope.details = function (recipe) {
      $scope.recipes.forEach(function(theRecipe) {
        theRecipe.active = false;
      });

      recipe.active = true;
      $rootScope.$broadcast('recipe:details', recipe);
    };
  }
})();
