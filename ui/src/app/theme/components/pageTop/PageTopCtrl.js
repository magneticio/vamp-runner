(function () {
  'use strict';

  angular.module('VampRunner.theme.components')
      .controller('PageTopCtrl', PageTopCtrl);

  /** @ngInject */
  function PageTopCtrl($scope) {

    $scope.load = {
      'cpu': 57.5,
      'heap': {
        'current': 635,
        'max': 1024
      }
    };
  }
})();
