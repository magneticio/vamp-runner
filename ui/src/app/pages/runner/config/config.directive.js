(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .directive('config', config);

  /** @ngInject */
  function config() {
    return {
      restrict: 'E',
      controller: 'ConfigCtrl',
      templateUrl: 'app/pages/runner/config/config.html'
    };
  }
})();