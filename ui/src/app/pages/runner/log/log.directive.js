(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .directive('log', log);

  /** @ngInject */
  function log() {
    return {
      restrict: 'E',
      controller: 'LogCtrl',
      templateUrl: 'app/pages/runner/log/log.html'
    };
  }
})();