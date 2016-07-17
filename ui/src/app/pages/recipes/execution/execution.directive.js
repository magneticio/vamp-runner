(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .directive('execution', execution);

  /** @ngInject */
  function execution() {
    return {
      restrict: 'E',
      controller: 'ExecutionCtrl',
      templateUrl: 'app/pages/recipes/execution/execution.html'
    };
  }
})();