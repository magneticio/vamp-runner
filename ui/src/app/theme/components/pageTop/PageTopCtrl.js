(function () {
  'use strict';

  angular.module('VampRunner.theme.components')
    .controller('PageTopCtrl', PageTopCtrl);

  /** @ngInject */
  function PageTopCtrl($rootScope, $scope) {

    $scope.error = false;
    $scope.connected = false;

    $scope.load = {
      cpu: 0,
      heap: {
        max: 0,
        used: 0,
        percentage: 0
      }
    };

    $rootScope.$on('vamp:load', function (event, data) {
      $scope.error = false;
      if (!$scope.connected) $scope.connected = true;
      $scope.load = data;
    });

    $rootScope.$on('vamp:error', function (event, data) {
      $scope.connected = false;
      $scope.error = true;
    });
  }
})();
