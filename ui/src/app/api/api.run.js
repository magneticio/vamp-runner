(function () {
  'use strict';

  angular.module('VampRunner.api')
    .run(["api", function (api) {
      api.init();
    }]);

})();