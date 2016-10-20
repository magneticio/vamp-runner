(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .controller('ListCtrl', ListCtrl);

  /** @ngInject */
  function ListCtrl($rootScope, $scope, baConfig, $runner) {
    $scope.transparent = baConfig.theme.blur;
    $scope.recipes = $runner.recipes;

    $scope.details = function (recipe) {
      $scope.recipes.forEach(function(theRecipe) {
        theRecipe.active = false;
      });

      recipe.active = true;
      $rootScope.$broadcast('recipe:details', recipe);
    };
  }
})();
