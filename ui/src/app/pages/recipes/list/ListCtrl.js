(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .controller('RecipesCtrl', RecipesCtrl);

  /** @ngInject */
  function RecipesCtrl($scope, baConfig, api, toastr) {

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

    $scope.isRunning = function () {
      for (var i = 0; i < api.recipes.length; i++) {
        if (api.recipes[i].state === 'running') {
          return true;
        }
      }
      return false;
    };

    $scope.run = function () {
      api.run();
    };

    $scope.stop = function () {
      api.stop();
    };

    $scope.purge = function () {
      api.purge();
    };

    $scope.details = function (recipe) {
      toastr.info('', recipe.title);
    };
  }
})();