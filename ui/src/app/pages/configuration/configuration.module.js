(function () {
  'use strict';

  angular.module('VampRunner.pages.configuration', [])
      .config(routeConfig);

  /** @ngInject */
  function routeConfig($stateProvider) {
    $stateProvider
        .state('configuration', {
          url: '/configuration',
          templateUrl: 'app/pages/configuration/configuration.html',
          title: 'Configuration',
          sidebarMeta: {
            icon: 'ion-ios-gear',
            order: 20
          }
        });
  }

})();
