/**
 * Created by k.danovsky on 13.05.2016.
 */

(function () {
  'use strict';

  angular.module('VampRunner.theme')
    .config(config);

  /** @ngInject */
  function config(baConfigProvider) {
    baConfigProvider.changeTheme({blur: true});

    baConfigProvider.changeColors({
      default: 'rgba(#000000, 0.2)',
      defaultText: '#ffffff',
      all: {
        white: '#ffffff'
      }
    });
  }
})();
