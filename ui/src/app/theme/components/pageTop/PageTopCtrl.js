(function () {
  'use strict';

  angular.module('VampRunner.theme.components')
      .controller('PageTopCtrl', PageTopCtrl);

  /** @ngInject */
  function PageTopCtrl($rootScope, $scope) {

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
      if (!$scope.connected) $scope.connected = true;
      $scope.load = data;
    });
  }
})();
