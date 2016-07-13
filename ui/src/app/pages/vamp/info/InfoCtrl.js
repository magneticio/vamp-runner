(function () {
  'use strict';

  angular.module('VampRunner.pages.vamp')
      .controller('InfoCtrl', InfoCtrl);

  /** @ngInject */
  function InfoCtrl($scope) {

    $scope.items = [
      {
        'name': 'Message',
        'value': 'Hi, I\'m Vamp! How are you?',
        'order': 0
      },
      {
        'name': 'Version',
        'value': '0.8.5-132-g3668da6',
        'order': 1
      },
      {
        'name': 'Persistence',
        'value': 'Elasticsearch',
        'order': 2
      },
      {
        'name': 'Key-Value Store',
        'value': 'ZooKeeper',
        'order': 3
      },
      {
        'name': 'Gateway Driver',
        'value': 'HAProxy 1.6.x',
        'order': 4
      },
      {
        'name': 'Container Driver',
        'value': 'Marathon',
        'order': 5
      },
      {
        'name': 'Workflow Driver',
        'value': 'Marathon,Chronos',
        'order': 6
      }
    ];
  }
})();
