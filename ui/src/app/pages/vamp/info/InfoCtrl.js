(function () {
  'use strict';

  angular.module('VampRunner.pages.vamp')
      .controller('InfoCtrl', InfoCtrl);

  /** @ngInject */
  function InfoCtrl($scope) {

    $scope.items = [
      {
        'name': 'version',
        'value': '0.8.5-132-g3668da6',
        'order': 1
      },
      {
        'name': 'persistence',
        'value': 'elasticsearch',
        'order': 2
      },
      {
        'name': 'key-value store',
        'value': 'zookeeper',
        'order': 3
      },
      {
        'name': 'gateway driver',
        'value': 'haproxy 1.6.x',
        'order': 4
      },
      {
        'name': 'container driver',
        'value': 'marathon',
        'order': 5
      },
      {
        'name': 'workflow driver',
        'value': 'marathon,chronos',
        'order': 6
      }
    ];
  }
})();
