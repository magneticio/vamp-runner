(function () {
  'use strict';

  angular.module('VampRunner.pages.all')
      .directive('execution', execution);

  /** @ngInject */
  function execution() {
    return {
      restrict: 'E',
      controller: 'ExecutionCtrl',
      templateUrl: 'app/pages/all/execution/execution.html'
    };
  }
})();