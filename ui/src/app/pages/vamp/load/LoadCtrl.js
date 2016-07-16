(function () {
  'use strict';

  angular.module('VampRunner.pages.vamp')
    .controller('LoadCtrl', LoadCtrl);

  /** @ngInject */
  function LoadCtrl($rootScope, $scope, $filter, api) {

    $scope.labels = [];
    $scope.data = [
      [],
      []
    ];
    $scope.series = ['CPU', 'HEAP'];

    var length = 30;

    function tail(array, value, padding) {
      array.push(value);
      while (array.length > length) array.shift();
      while (array.length < length) array.unshift(padding);
    }

    function onLoad(load) {
      tail($scope.labels, $filter('date')(Date.now(), 'HH:mm:ss'), ".");
      tail($scope.data[0], $filter('number')(load.cpu, 2), 0);
      tail($scope.data[1], $filter('number')(load.heap.percentage, 2), 0);
    }

    tail($scope.labels, ".", ".");
    tail($scope.data[0], 0, 0);
    tail($scope.data[1], 0, 0);

    api.loads.forEach(function (load) {
      onLoad(load);
    });

    $rootScope.$on('vamp:load', function (event, load) {
      onLoad(load);
    });
  }

})();
