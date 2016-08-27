(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .controller('ListCtrl', ListCtrl);

  /** @ngInject */
  function ListCtrl($rootScope, $scope, baConfig, api) {

    $scope.transparent = baConfig.theme.blur;

    $scope.recipes = api.recipes;

    $scope.details = function (recipe) {
      $rootScope.$emit('recipe:details', recipe);
    };
  }
})();
