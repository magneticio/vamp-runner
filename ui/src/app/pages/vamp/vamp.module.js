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
          icon: 'ion-information-circled',
          order: 0
        }
      });
  }

  function chartJsConfig(ChartJsProvider, baConfigProvider) {
    var layoutColors = baConfigProvider.colors;
    ChartJsProvider.setOptions({
      colours: [ layoutColors.info, layoutColors.success, layoutColors.default, layoutColors.primary],
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
