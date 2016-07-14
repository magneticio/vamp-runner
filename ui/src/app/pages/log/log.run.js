(function () {
  'use strict';

  angular.module('VampRunner.pages.log')
    .run(["log", function (log) {
      return log.entries;
    }]);

})();