(function () {
  'use strict';

  angular.module('VampRunner.pages.runner')
    .controller('RecipesCtrl', RecipesCtrl);

  /** @ngInject */
  function RecipesCtrl($rootScope, $scope, baConfig, api) {

    $scope.transparent = baConfig.theme.blur;

    $scope.recipes = api.recipes;

    var findById = function (id) {
      for (var i = 0; i < api.recipes.length; i++) {
        var recipe = api.recipes[i];
        if (recipe.id === id) return recipe;
      }
    };

    var updateSelected = function (action, recipe) {
      if (action === 'add' && !recipe.selected) {
        recipe.selected = true;
      }
      if (action === 'remove' && recipe.selected) {
        recipe.selected = false;
      }
    };

    $scope.updateSelection = function ($event, id) {
      var checkbox = $event.target;
      var action = (checkbox.checked ? 'add' : 'remove');
      updateSelected(action, findById(id));
    };

    $scope.selectAll = function ($event) {
      var checkbox = $event.target;
      var action = (checkbox.checked ? 'add' : 'remove');
      for (var i = 0; i < api.recipes.length; i++) {
        updateSelected(action, api.recipes[i]);
      }
    };

    $scope.getSelectedClass = function (recipe) {
      return $scope.isSelected(recipe.id) ? 'selected' : '';
    };

    $scope.isSelected = function (id) {
      return findById(id).selected;
    };

    $scope.isSelectedAll = function () {
      var count = 0;
      api.recipes.forEach(function (recipe) {
        if (recipe.selected) count++;
      });
      return count === api.recipes.length;
    };

    //

    $scope.action = 'ready';

    function refresh() {
      var action = 'ready';
      for (var i = 0; i < api.recipes.length; i++) {
        if (api.recipes[i].state === 'running') {
          action = 'stop';
          break;
        }
      }
      $scope.action = action;
    }

    $scope.executeAction = function () {
      if ($scope.action == 'ready')
        api.run();
      else if ($scope.action == 'stop')
        api.stop();
    };

    $rootScope.$on('recipes:update', function () {
      refresh();
    });

    refresh();
  }
})();
