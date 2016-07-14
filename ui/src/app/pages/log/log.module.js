(function () {
  'use strict';

  angular.module('VampRunner.pages.log', [])
    .config(routeConfig);

  /** @ngInject */
  function routeConfig($stateProvider) {
    $stateProvider
      .state('log', {
        url: '/log',
        templateUrl: 'app/pages/log/log.html',
        title: 'Log',
        sidebarMeta: {
          icon: 'ion-ios-infinite',
          order: 30
        }
      });
  }

})();
