(function () {
  'use strict';

  angular.module('VampRunner.pages.vamp', []).config(routeConfig);

  /** @ngInject */
  function routeConfig($stateProvider) {
    $stateProvider
      .state('vamp', {
        url: '/vamp',
        title: 'Vamp',
        templateUrl: 'app/pages/vamp/vamp.html',
        sidebarMeta: {
          icon: 'ion-information-circled',
          order: 0
        }
      });
  }

})();
