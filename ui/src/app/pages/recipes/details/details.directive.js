(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .directive('details', details);

  /** @ngInject */
  function details() {
    return {
      restrict: 'EA',
      controller: 'DetailsCtrl',
      templateUrl: 'app/pages/recipes/details/details.html'
    };
  }
})();