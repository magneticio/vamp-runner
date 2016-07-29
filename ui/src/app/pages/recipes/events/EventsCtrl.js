(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .controller('EventsCtrl', EventsCtrl);

  /** @ngInject */
  function EventsCtrl($rootScope, $scope, baConfig, api) {

    $scope.transparent = baConfig.theme.blur;

    var events = $scope.events = [];
    var eventsAfter = 0;

    for (var i = 0; i < api.events.length; i++) {
      var event = api.events[i];
      if (event.timestamp > eventsAfter) events.unshift(event);
    }

    while (events.length > 30) events.pop();

    $scope.clear = function () {
      eventsAfter = events.length > 0 ? events[0].timestamp : 0;
      events.length = 0;
    };

    $rootScope.$on('vamp:event', function (event, e) {
      if (e.timestamp > eventsAfter) events.unshift(e);
      while (events.length > 30) events.pop();
    });
  }
})();
