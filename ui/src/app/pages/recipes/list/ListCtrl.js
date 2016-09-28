(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .controller('ListCtrl', ListCtrl);

  /** @ngInject */
  function ListCtrl($rootScope, $scope, baConfig, api) {


    $scope.transparent = baConfig.theme.blur;

    $scope.recipes = api.recipes;

    $scope.details = function (recipe) {
      $scope.recipes.forEach(function(theRecipe) {
        theRecipe.active = false;
      })

      recipe.active = true;

      console.log(recipe.active);

      $rootScope.$emit('recipe:details', recipe);
    };
  }
})();
