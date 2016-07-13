(function () {
  'use strict';

  angular.module('VampRunner.pages.runner')
      .directive('execution', execution);

  /** @ngInject */
  function execution() {
    return {
      restrict: 'E',
      controller: 'ExecutionCtrl',
      templateUrl: 'app/pages/runner/execution/execution.html'
    };
  }
})();