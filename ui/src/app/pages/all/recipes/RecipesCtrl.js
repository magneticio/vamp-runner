(function () {
  'use strict';

  angular.module('VampRunner.pages.all')
      .controller('RecipesCtrl', RecipesCtrl);

  /** @ngInject */
  function RecipesCtrl($scope, baConfig) {

    $scope.transparent = baConfig.theme.blur;

    $scope.action = 'stop';

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
        title: 'Route Weights', state: 'running'
      },
      {
        title: 'Route Weights with Condition Strength', state: 'ready'
      },
      {
        title: 'Scaling In/Out', state: 'ready'
      }
    ];

    $scope.executeAction = function(){
      $scope.action = ($scope.action == 'ready' ? 'stop' : 'ready');
    };
  }
})();
