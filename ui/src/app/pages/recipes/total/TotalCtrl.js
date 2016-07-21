(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .controller('TotalCtrl', TotalCtrl);

  /** @ngInject */
  function TotalCtrl($rootScope, $scope, baConfig, colorHelper, api) {

    $scope.transparent = baConfig.theme.blur;

    var refresh = function () {
      var count = $scope.count = api.recipes.length;

      var succeeded = 0;
      var failed = 0;
      var running = 0;
      var aborted = 0;
      var idle = 0;

      for (var i = 0; i < api.recipes.length; i++) {
        var recipe = api.recipes[i];
        if (recipe.state === 'succeeded')
          succeeded++;
        else if (recipe.state === 'failed')
          failed++;
        else if (recipe.state === 'running')
          running++;
        else if (recipe.state === 'aborted')
          aborted++;
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
          value: aborted,
          color: allColors.gray,
          highlight: colorHelper.shade(allColors.gray, 15),
          label: 'Aborted',
          percentage: percentage(aborted),
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

    $rootScope.$on('recipes:update', function () {
      refresh();
    });
  }
})();