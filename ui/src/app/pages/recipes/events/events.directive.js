(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .directive('events', events);

  /** @ngInject */
  function events() {
    return {
      restrict: 'EA',
      controller: 'EventsCtrl',
      templateUrl: 'app/pages/recipes/events/events.html'
    };
  }
})();