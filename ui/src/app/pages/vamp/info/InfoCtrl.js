(function () {
  'use strict';

  angular.module('VampRunner.pages.vamp')
    .controller('InfoCtrl', InfoCtrl);

  /** @ngInject */
  function InfoCtrl($scope, $runner) {

    function refresh() {
      function get(obj) {
        return obj ? obj : '...............';
      }

      $scope.items = [
        {
          'name': 'version',
          'value': get($runner.info.version)
        },
        {
          'name': 'persistence',
          'value': get($runner.info.persistence)
        },
        {
          'name': 'key-value store',
          'value': get($runner.info.key_value_store)
        },
        {
          'name': 'gateway driver',
          'value': get($runner.info.gateway_driver)
        },
        {
          'name': 'container driver',
          'value': get($runner.info.container_driver)
        },
        {
          'name': 'workflow driver',
          'value': get($runner.info.workflow_driver)
        }
      ];
    }

    var cancel = $scope.$on('vamp:info', function () {
      refresh();
      cancel();
    });

    refresh();
  }
})();
