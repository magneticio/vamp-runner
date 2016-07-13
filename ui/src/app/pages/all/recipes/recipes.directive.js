(function () {
  'use strict';

  angular.module('VampRunner.pages.all')
      .directive('recipes', recipes);

  /** @ngInject */
  function recipes() {
    return {
      restrict: 'EA',
      controller: 'RecipesCtrl',
      templateUrl: 'app/pages/all/recipes/recipes.html'
    };
  }
})();