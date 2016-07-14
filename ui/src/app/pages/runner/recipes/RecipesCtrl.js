(function () {
  'use strict';

  angular.module('VampRunner.pages.runner')
    .controller('RecipesCtrl', RecipesCtrl);

  /** @ngInject */
  function RecipesCtrl($scope, baConfig) {

    $scope.transparent = baConfig.theme.blur;

    $scope.action = 'stop';

    $scope.recipes = [
      {
        id: "1",
        title: 'HTTP Deployment',
        state: 'success'
      },
      {
        id: "2",
        title: 'HTTP Canary',
        state: 'success'
      },
      {
        id: "3",
        title: 'HTTP with Dependencies',
        state: 'failure'
      },
      {
        id: "4",
        title: 'HTTP Flip-Flop Versions',
        state: 'success'
      },
      {
        id: "5",
        title: 'HTTP Flip-Flop Versions with Dependencies',
        state: 'failure'
      },
      {
        id: "6",
        title: 'TCP Deployment',
        state: 'success'
      },
      {
        id: "7",
        title: 'TCP with Dependencies',
        state: 'failure'
      },
      {
        id: "8",
        title: 'Route Weights',
        state: 'running'
      },
      {
        id: "9",
        title: 'Route Weights with Condition Strength',
        state: 'ready'
      },
      {
        id: "10",
        title: 'Scaling In/Out',
        state: 'ready'
      }
    ];

    var selected = $scope.selected = ["1", "3"];

    var updateSelected = function (action, id) {
      if (action === 'add' && $scope.selected.indexOf(id) === -1) {
        $scope.selected.push(id);
      }
      if (action === 'remove' && $scope.selected.indexOf(id) !== -1) {
        $scope.selected.splice($scope.selected.indexOf(id), 1);
      }
    };

    $scope.updateSelection = function ($event, id) {
      var checkbox = $event.target;
      var action = (checkbox.checked ? 'add' : 'remove');
      updateSelected(action, id);
    };

    $scope.selectAll = function ($event) {
      var checkbox = $event.target;
      var action = (checkbox.checked ? 'add' : 'remove');
      for (var i = 0; i < $scope.recipes.length; i++) {
        var recipe = $scope.recipes[i];
        updateSelected(action, recipe.id);
      }
    };

    $scope.getSelectedClass = function (recipe) {
      return $scope.isSelected(recipe.id) ? 'selected' : '';
    };

    $scope.isSelected = function (id) {
      return $scope.selected.indexOf(id) >= 0;
    };

    $scope.isSelectedAll = function () {
      return $scope.selected.length === $scope.recipes.length;
    };

    //

    $scope.executeAction = function () {
      $scope.action = ($scope.action == 'ready' ? 'stop' : 'ready');
    };
  }
})();
