(function () {
  'use strict';

  angular.module('VampRunner.pages.runner')
      .controller('RunnerCtrl', RunnerCtrl);

  /** @ngInject */
  function RunnerCtrl($rootScope, $scope, runner) {

    $scope.logs = runner.logs;

    $scope.class = function (log) {
      var color = log.level === 'info' ? 'primary' : (log.level === 'error' ? 'danger' : 'warning');
      var align = log.source === 'user' ? '' : ' cd-timeline-right';
      return color + align;
    };

    $scope.image = function (log) {
      return log.level === 'info' ? 'primary' : (log.level === 'error' ? 'danger' : 'warning');
    };

    $scope.source = function (log) {
      return log.source === 'user' ? 'Programming' : 'Checklist';
    };

    $scope.title = function (log) {
      return log.level;
    };

    $rootScope.$on('log', function () {
      $scope.logs = runner.logs;
    });

    var timelineBlocks = $('.cd-timeline-block'), offset = 0.8;

    hideBlocks(timelineBlocks, offset);

    $(window).on('scroll', function () {
      if (!window.requestAnimationFrame) {
        setTimeout(function () {
          showBlocks(timelineBlocks, offset);
        }, 100);
      } else {
        window.requestAnimationFrame(function () {
          showBlocks(timelineBlocks, offset);
        });
      }
    });

    function hideBlocks(blocks, offset) {
      blocks.each(function () {
        ( $(this).offset().top > $(window).scrollTop() + $(window).height() * offset ) && $(this).find('.cd-timeline-img, .cd-timeline-content').addClass('is-hidden');
      });
    }

    function showBlocks(blocks, offset) {
      blocks.each(function () {
        ( $(this).offset().top <= $(window).scrollTop() + $(window).height() * offset && $(this).find('.cd-timeline-img').hasClass('is-hidden') ) && $(this).find('.cd-timeline-img, .cd-timeline-content').removeClass('is-hidden').addClass('bounce-in');
      });
    }
  }
})();