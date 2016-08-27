(function () {
  'use strict';

  angular.module('VampRunner.pages.runner')
    .run(["log", function (log) {
      return log.logs;
    }]);

})();