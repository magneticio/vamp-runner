(function () {
  'use strict';

  angular.module('VampRunner.pages.all')
      .controller('RecipesCtrl', RecipesCtrl);

  /** @ngInject */
  function RecipesCtrl($scope, baConfig) {

    $scope.transparent = baConfig.theme.blur;

    $scope.recipes = [
      { 
        title: 'HTTP Deployment', state: 'success'
      },
      {
        title: 'HTTP Canary', state: 'success'
      },
      {
        title: 'HTTP with Dependencies', state: 'failure'
      },
      {
        title: 'HTTP Flip-Flop Versions', state: 'success'
      },
      {
        title: 'HTTP Flip-Flop Versions with Dependencies', state: 'failure'
      },
      {
        title: 'TCP Deployment', state: 'success'
      },
      {
        title: 'TCP with Dependencies', state: 'success'
      },
      {
        title: 'Route Weights'
      },
      {
        title: 'Route Weights with Condition Strength'
      },
      {
        title: 'Scaling In/Out'
      }
    ];
  }
})();
