(function () {
  'use strict';
  angular.module('VampRunner.api')
    .run(["$runner", function ($runner) {
      $runner.init();
    }]);
})();