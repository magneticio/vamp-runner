(function () {
  'use strict';

  angular.module('VampRunner.pages.vamp')
    .controller('InfoCtrl', InfoCtrl);

  /** @ngInject */
  function InfoCtrl($rootScope, $scope, vamp) {

    function refresh() {
      function get(obj) {
        return obj ? obj : '...............';
      }

      $scope.items = [
        {
          'name': 'version',
          'value': get(vamp.info.version)
        },
        {
          'name': 'persistence',
          'value': get(vamp.info.persistence)
        },
        {
          'name': 'key-value store',
          'value': get(vamp.info.key_value_store)
        },
        {
          'name': 'gateway driver',
          'value': get(vamp.info.gateway_driver)
        },
        {
          'name': 'container driver',
          'value': get(vamp.info.container_driver)
        },
        {
          'name': 'workflow driver',
          'value': get(vamp.info.workflow_driver)
        }
      ];
    }

    var cancel = $rootScope.$on('vamp:info', function () {
      refresh();
      cancel();
    });

    refresh();
  }
})();
