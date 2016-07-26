(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .directive('steps', steps);

  /** @ngInject */
  function steps() {
    return {
      restrict: 'EA',
      controller: 'StepsCtrl',
      templateUrl: 'app/pages/recipes/steps/steps.html'
    };
  }
})();