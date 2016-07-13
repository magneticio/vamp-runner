(function () {
  'use strict';

  angular.module('VampRunner.pages.vamp')
    .controller('LoadCtrl', LoadCtrl);

  /** @ngInject */
  function LoadCtrl($scope) {
    $scope.labels =["-9", "-8", "-7", "-6", "-5", "-4", "-3", "-2", "-1", "-0"];
    $scope.data = [
      [65, 59, 90, 81, 56, 55, 40, 56, 55, 40, 65, 59, 90, 81, 56, 55, 40, 56, 55, 40],
      [28, 48, 40, 19, 88, 27, 45, 88, 27, 45, 65, 59, 90, 81, 56, 55, 40, 56, 55, 40]
    ];
    $scope.series = ['CPU', 'HEAP'];

    $scope.changeData = function () {
      $scope.data[0] = shuffle($scope.data[0]);
      $scope.data[1] = shuffle($scope.data[1]);
    };

    $scope.options = {
      'bezierCurve': false
    };

    function shuffle(o){
      for(var j, x, i = o.length; i; j = Math.floor(Math.random() * i), x = o[--i], o[i] = o[j], o[j] = x){}
      return o;
    }
  }

})();
