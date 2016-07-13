/**
 * @author v.lugovksy
 * created on 16.12.2015
 */
(function () {
  'use strict';

  angular.module('VampRunner.theme.components')
      .directive('pageTop', pageTop);

  /** @ngInject */
  function pageTop() {
    return {
      restrict: 'E',
      controller: 'PageTopCtrl',
      templateUrl: 'app/theme/components/pageTop/pageTop.html'
    };
  }

})();