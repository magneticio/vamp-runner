(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .directive('list', list);

  /** @ngInject */
  function list() {
    return {
      restrict: 'EA',
      controller: 'ListCtrl',
      templateUrl: 'app/pages/recipes/list/list.html'
    };
  }
})();