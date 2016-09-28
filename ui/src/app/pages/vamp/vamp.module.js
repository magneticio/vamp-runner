(function () {
  'use strict';

  angular.module('VampRunner.pages.vamp', []).config(routeConfig).config(chartJsConfig);

  /** @ngInject */
  function routeConfig($stateProvider) {
    $stateProvider
      .state('vamp', {
        url: '/vamp',
        title: 'Vamp',
        templateUrl: 'app/pages/vamp/vamp.html',
        sidebarMeta: {
          icon: 'ion-ios-information',
          order: 0
        }
      });
  }

  function chartJsConfig(ChartJsProvider, baConfigProvider) {
    var layoutColors = baConfigProvider.colors;
    ChartJsProvider.setOptions({
      colours: [ layoutColors.all.yellow, layoutColors.all.green, layoutColors.default, layoutColors.primary],
      responsive: true,
      scaleFontColor: layoutColors.defaultText,
      scaleLineColor: layoutColors.border,
      pointLabelFontColor: layoutColors.defaultText
    });
    ChartJsProvider.setOptions('Line', {
      datasetFill: true,
      bezierCurve: false
    });
  }

})();
