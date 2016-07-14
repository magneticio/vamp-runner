(function () {
  'use strict';

  angular.module('VampRunner.pages.log')
      .controller('LogCtrl', LogCtrl);

  /** @ngInject */
  function LogCtrl($rootScope, $scope, log) {

    $scope.entries = log.entries;

    $scope.class = function (entry) {
      var color = entry.level === 'info' ? 'primary' : (entry.level === 'error' ? 'danger' : 'warning');
      var align = entry.source === 'user' ? '' : ' cd-timeline-right';
      return color + align;
    };

    $scope.image = function (entry) {
      return entry.level === 'info' ? 'primary' : (entry.level === 'error' ? 'danger' : 'warning');
    };

    $scope.source = function (entry) {
      return entry.source === 'user' ? 'Programming' : 'Checklist';
    };

    $scope.title = function (entry) {
      return entry.level;
    };

    $rootScope.$on('log', function () {
      $scope.entries = log.entries;
    });

    var timelineBlocks = $('.cd-timeline-block'),
        offset = 0.8;

    //hide timeline blocks which are outside the viewport
    hideBlocks(timelineBlocks, offset);

    //on scolling, show/animate timeline blocks when enter the viewport
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