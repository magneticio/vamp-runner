(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes', [])
    .config(routeConfig);

  /** @ngInject */
  function routeConfig($stateProvider) {
    $stateProvider
      .state('recipes', {
        url: '/recipes',
        templateUrl: 'app/pages/recipes/recipes.html',
        title: 'Recipes',
        sidebarMeta: {
          icon: 'ion-play',
          order: 10
        }
      });
  }

})();
