(function () {
  'use strict';

  angular.module('VampRunner.pages.vamp')
      .directive('load', load);

  /** @ngInject */
  function load() {
    return {
      restrict: 'EA',
      controller: 'LoadCtrl',
      templateUrl: 'app/pages/vamp/load/load.html'
    };
  }
})();