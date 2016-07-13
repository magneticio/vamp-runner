(function () {
  'use strict';

  angular.module('VampRunner.pages.vamp')
      .directive('info', info);

  /** @ngInject */
  function info() {
    return {
      restrict: 'EA',
      controller: 'InfoCtrl',
      templateUrl: 'app/pages/vamp/info/info.html'
    };
  }
})();