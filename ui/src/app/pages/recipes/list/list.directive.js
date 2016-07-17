(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .directive('recipes', recipes);

  /** @ngInject */
  function recipes() {
    return {
      restrict: 'EA',
      controller: 'RecipesCtrl',
      templateUrl: 'app/pages/recipes/list/list.html'
    };
  }
})();