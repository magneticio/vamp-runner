'use strict';

angular.module('VampRunner', [
  'ngWebSocket',
  'ngAnimate',
  'ui.bootstrap',
  'ui.sortable',
  'ui.router',
  'ngTouch',
  'toastr',
  'ui.slimscroll',
  'angular-progress-button-styles',

  'VampRunner.api',
  'VampRunner.theme',
  'VampRunner.pages'
]).config(function (toastrConfig) {
  angular.extend(toastrConfig, {
    autoDismiss: true,
    timeOut: 5000,
    extendedTimeOut: 0,
    allowHtml: false,
    closeButton: true,
    tapToDismiss: true,
    positionClass: 'toast-bottom-right',
    preventOpenDuplicates: true
  });
});