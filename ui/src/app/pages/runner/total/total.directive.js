(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .directive('total', total);

  /** @ngInject */
  function total() {
    return {
      restrict: 'E',
      controller: 'TotalCtrl',
      templateUrl: 'app/pages/runner/total/total.html'
    };
  }
})();