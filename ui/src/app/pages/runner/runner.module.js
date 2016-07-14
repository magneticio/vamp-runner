(function () {
  'use strict';

  angular.module('VampRunner.pages.runner', [])
    .config(routeConfig);

  /** @ngInject */
  function routeConfig($stateProvider) {
    $stateProvider
      .state('runner', {
        url: '/runner',
        templateUrl: 'app/pages/runner/runner.html',
        title: 'Runner',
        sidebarMeta: {
          icon: 'ion-play',
          order: 10
        }
      });
  }

})();
