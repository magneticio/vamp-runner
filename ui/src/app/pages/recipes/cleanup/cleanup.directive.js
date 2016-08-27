(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .directive('cleanup', cleanup);

  /** @ngInject */
  function cleanup() {
    return {
      restrict: 'EA',
      controller: 'CleanupCtrl',
      templateUrl: 'app/pages/recipes/cleanup/cleanup.html'
    };
  }
})();