(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .controller('DetailsCtrl', DetailsCtrl);

  /** @ngInject */
  function DetailsCtrl($rootScope, $scope, baConfig, api) {

    $scope.transparent = baConfig.theme.blur;

    $scope.recipe = api.recipes[0];

    $rootScope.$on('recipe:details', function (event, recipe) {
      $scope.recipe = recipe;
    });
  }
})();
