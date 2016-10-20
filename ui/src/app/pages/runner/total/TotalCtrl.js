(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .controller('TotalCtrl', TotalCtrl);

  /** @ngInject */
  function TotalCtrl($scope, baConfig, colorHelper, $runner) {

    $scope.transparent = baConfig.theme.blur;

    var refresh = function () {
      var count = $scope.count = $runner.recipes.length;

      var succeeded = 0;
      var failed = 0;
      var running = 0;
      var idle = 0;

      for (var i = 0; i < $runner.recipes.length; i++) {
        var recipe = $runner.recipes[i];
        if (recipe.state === 'succeeded')
          succeeded++;
        else if (recipe.state === 'failed')
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
          value: succeeded,
          color: allColors.green,
          highlight: colorHelper.shade(allColors.green, 15),
          label: 'Succeeded',
          percentage: percentage(succeeded),
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

      var ctx = document.getElementById('total-chart-area').getContext('2d');
      window.totalDoughnut = new Chart(ctx).Doughnut($scope.doughnutData, {
        segmentShowStroke: true,
        percentageInnerCutout: 64,
        responsive: true
      });
    };

    refresh();

    $scope.$on('recipes:update', function () {
      refresh();
    });
  }
})();