(function () {
  'use strict';

  angular.module('VampRunner.pages.recipes')
    .controller('EventsCtrl', EventsCtrl);

  /** @ngInject */
  function EventsCtrl($rootScope, $scope, baConfig) {

    $scope.transparent = baConfig.theme.blur;

    var events = $scope.events = [];

    $scope.clear = function () {
      console.log("clear");
      events.length = 0;
    };

    $rootScope.$on('vamp:event', function (event, data) {
      var e = {
        timestamp: new Date(),
        tags: data.tags.join(', '),
        value: data.value
      };
      events.unshift(e);
    });
  }
})();
