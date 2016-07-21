(function () {
  'use strict';

  angular.module('VampRunner.pages', [
    'ui.router',
    'VampRunner.pages.vamp',
    'VampRunner.pages.recipes',
    'VampRunner.pages.runner'
  ])
    .config(routeConfig);

  /** @ngInject */
  function routeConfig($urlRouterProvider) {
    $urlRouterProvider.otherwise('/vamp');
  }

})();
