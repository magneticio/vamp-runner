/**
 * @author v.lugovsky
 * created on 16.12.2015
 */
(function () {
  'use strict';

  angular.module('VampRunner.pages', [
    'ui.router',

    'VampRunner.pages.dashboard',
    'VampRunner.pages.config',
    'VampRunner.pages.log'
  ])
      .config(routeConfig);

  /** @ngInject */
  function routeConfig($urlRouterProvider, baSidebarServiceProvider) {
    $urlRouterProvider.otherwise('/dashboard');
  }

})();
