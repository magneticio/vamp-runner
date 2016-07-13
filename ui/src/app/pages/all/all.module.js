(function () {
  'use strict';

  angular.module('VampRunner.pages.all', [])
      .config(routeConfig);

  /** @ngInject */
  function routeConfig($stateProvider) {
    $stateProvider
        .state('all', {
          url: '/all',
          templateUrl: 'app/pages/all/all.html',
          title: 'Execute All',
          sidebarMeta: {
            icon: 'ion-ios-star',
            order: 0
          }
        });
  }

})();
