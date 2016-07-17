(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .controller('ExecutionCtrl', ExecutionCtrl);

  /** @ngInject */
  function ExecutionCtrl($rootScope, $scope, baConfig, colorHelper, api) {

    $scope.transparent = baConfig.theme.blur;

    var refresh = function () {
      var count = $scope.count = api.recipes.length;

      var completed = 0;
      var failed = 0;
      var running = 0;
      var idle = 0;

      for (var i = 0; i < api.recipes.length; i++) {
        var recipe = api.recipes[i];
        if (recipe.state === 'success')
          completed++;
        else if (recipe.state === 'failure')
          failed++;
        else if (recipe.state === 'running')
          running++;
        else if (recipe.state === 'idle')
          idle++;
      }

      var percentage = function (value) {
        return value === 0 ? 0 : 100 * value / count;
      };

      var allColors = baConfig.colors.all;
      $scope.doughnutData = [
        {
          value: completed,
          color: allColors.green,
          highlight: colorHelper.shade(allColors.green, 15),
          label: 'Completed',
          percentage: percentage(completed),
          order: 0
        }, {
          value: failed,
          color: allColors.red,
          highlight: colorHelper.shade(allColors.red, 15),
          label: 'Failed',
          percentage: percentage(failed),
          order: 1
        }, {
          value: running,
          color: allColors.yellow,
          highlight: colorHelper.shade(allColors.yellow, 15),
          label: 'Running',
          percentage: percentage(running),
          order: 2
        }, {
          value: idle,
          color: allColors.blue,
          highlight: colorHelper.shade(allColors.blue, 15),
          label: 'Idle',
          percentage: percentage(idle),
          order: 3
        }
      ];

      var ctx = document.getElementById('execution-chart-area').getContext('2d');
      window.executionDoughnut = new Chart(ctx).Doughnut($scope.doughnutData, {
        segmentShowStroke: true,
        percentageInnerCutout: 64,
        responsive: true
      });
    };

    refresh();

    $rootScope.$on('recipes:update', function () {
      refresh();
    });
  }
})();