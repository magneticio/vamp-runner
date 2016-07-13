(function () {
  'use strict';

  angular.module('VampRunner.pages.runner')
      .directive('recipes', recipes);

  /** @ngInject */
  function recipes() {
    return {
      restrict: 'EA',
      controller: 'RecipesCtrl',
      templateUrl: 'app/pages/runner/recipes/recipes.html'
    };
  }
})();