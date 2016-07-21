(function () {
  'use strict';

  angular.module('VampRunner.pages.runner')
    .run(["runner", function (runner) {
      return runner.logs;
    }]);

})();