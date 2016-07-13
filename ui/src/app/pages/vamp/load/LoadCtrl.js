(function () {
  'use strict';

  angular.module('VampRunner.pages.vamp')
    .controller('LoadCtrl', LoadCtrl);

  /** @ngInject */
  function LoadCtrl($rootScope, $scope, $filter, vamp) {

    $scope.labels =[];
    $scope.data = [
      [],
      []
    ];
    $scope.series = ['CPU', 'HEAP'];

    $scope.options = {
      'bezierCurve': false
    };

    var length = 30;

    function tail(array, value, padding) {
      array.push(value);
      if (array.length > length) array.shift();
      while (array.length < length) array.unshift(padding);
    }

    function onLoad(load) {
      tail($scope.labels, $filter('date')(new Date(), 'HH:mm:ss'), ".");
      tail($scope.data[0], load.cpu, 0);
      tail($scope.data[1], load.heap.percentage, 0);
    }

    tail($scope.labels, ".", ".");
    tail($scope.data[0], 0, 0);
    tail($scope.data[1], 0, 0);

    vamp.loads.forEach(function(load) {
      onLoad(load);
    });

    $rootScope.$on('vamp:load', function (event, load) {
      onLoad(load);
    });
  }

})();
