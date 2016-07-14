(function () {
  'use strict';

  angular.module('VampRunner.pages.vamp')
    .controller('InfoCtrl', InfoCtrl);

  /** @ngInject */
  function InfoCtrl($rootScope, $scope, api) {

    function refresh() {
      function get(obj) {
        return obj ? obj : '...............';
      }

      $scope.items = [
        {
          'name': 'version',
          'value': get(api.info.version)
        },
        {
          'name': 'persistence',
          'value': get(api.info.persistence)
        },
        {
          'name': 'key-value store',
          'value': get(api.info.key_value_store)
        },
        {
          'name': 'gateway driver',
          'value': get(api.info.gateway_driver)
        },
        {
          'name': 'container driver',
          'value': get(api.info.container_driver)
        },
        {
          'name': 'workflow driver',
          'value': get(api.info.workflow_driver)
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
