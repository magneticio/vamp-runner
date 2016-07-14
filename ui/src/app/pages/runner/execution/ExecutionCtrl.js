(function () {
  'use strict';

  angular.module('VampRunner.pages.runner')
    .controller('ExecutionCtrl', ExecutionCtrl);

  /** @ngInject */
  function ExecutionCtrl($scope, baConfig, colorHelper) {

    $scope.transparent = baConfig.theme.blur;
    var allColors = baConfig.colors.all;
    $scope.doughnutData = [
      {
        value: 5,
        color: allColors.green,
        highlight: colorHelper.shade(allColors.green, 15),
        label: 'Completed',
        percentage: 50,
        order: 0
      }, {
        value: 2,
        color: allColors.red,
        highlight: colorHelper.shade(allColors.red, 15),
        label: 'Failed',
        percentage: 20,
        order: 1
      }, {
        value: 1,
        color: allColors.yellow,
        highlight: colorHelper.shade(allColors.yellow, 15),
        label: 'Running',
        percentage: 10,
        order: 2
      }, {
        value: 2,
        color: allColors.blue,
        highlight: colorHelper.shade(allColors.blue, 15),
        label: 'Ready',
        percentage: 30,
        order: 3
      }
    ];

    var ctx = document.getElementById('execution-chart-area').getContext('2d');
    window.executionDoughnut = new Chart(ctx).Doughnut($scope.doughnutData, {
      segmentShowStroke: true,
      percentageInnerCutout: 64,
      responsive: true
    });
  }
})();