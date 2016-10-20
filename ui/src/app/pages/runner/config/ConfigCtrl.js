(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .controller('ConfigCtrl', ConfigCtrl);

  /** @ngInject */
  function ConfigCtrl($scope, $runner) {
    $scope.config = $runner.config;
  }
})();