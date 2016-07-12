(function () {
  'use strict';

  angular.module('VampRunner.pages.config', [])
      .config(routeConfig);

  /** @ngInject */
  function routeConfig($stateProvider) {
    $stateProvider
        .state('config', {
          url: '/config',
          templateUrl: 'app/pages/config/config.html',
          title: 'Config',
          sidebarMeta: {
            icon: 'ion-ios-gear',
            order: 0,
          },
        });
  }

})();
