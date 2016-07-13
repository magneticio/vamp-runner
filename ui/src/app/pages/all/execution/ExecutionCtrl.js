(function () {
  'use strict';

  angular.module('VampRunner.pages.all')
      .controller('ExecutionCtrl', ExecutionCtrl);

  /** @ngInject */
  function ExecutionCtrl($scope, baConfig, colorHelper) {

    $scope.transparent = baConfig.theme.blur;
    var allColors = baConfig.colors.all;
    $scope.doughnutData = [
      {
        value: 2000,
        color: allColors.green,
        highlight: colorHelper.shade(allColors.green, 15),
        label: 'Passed',
        percentage: 87,
        order: 0
      }, {
        value: 1500,
        color: allColors.red,
        highlight: colorHelper.shade(allColors.red, 15),
        label: 'Failed',
        percentage: 22,
        order: 1
      }, {
        value: 1000,
        color: allColors.yellow,
        highlight: colorHelper.shade(allColors.yellow, 15),
        label: 'Running',
        percentage: 70,
        order: 2
      }, {
        value: 1200,
        color: allColors.blue,
        highlight: colorHelper.shade(allColors.blue, 15),
        label: 'Remaining',
        percentage: 38,
        order: 3
      }
    ];

    var ctx = document.getElementById('chart-area').getContext('2d');
    window.myDoughnut = new Chart(ctx).Doughnut($scope.doughnutData, {
      segmentShowStroke: false,
      percentageInnerCutout : 64,
      responsive: true
    });
  }
})();