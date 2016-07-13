(function () {
  'use strict';

  angular.module('VampRunner.pages.vamp')
    .run(["vamp", function(vamp) {
      vamp.start();
    }]);

})();