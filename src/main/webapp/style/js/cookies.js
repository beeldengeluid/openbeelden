/*
  Javascript detecting cookie warning etc.
  @author:  Andre van Toly
  @version: '$Id$'
*/

var ready = function(fn) {
    if (document.readyState != "loading") {
      fn();
    } else {
      document.addEventListener("DOMContentLoaded", fn);
    }
  };
  
  var setCookie = function(name, value, exdays) {
    var exdate = new Date();
    exdate.setDate(exdate.getDate() + exdays);
    if (location.protocol === "https:") {
      value =
        escape(value) +
        (exdays == null ? "" : "; expires=" + exdate.toUTCString()) +
        "; secure";
    } else {
      value =
        escape(value) +
        (exdays == null ? "" : "; expires=" + exdate.toUTCString());
    }
    document.cookie = name + "=" + value;
  };
  
  var fadeOut = function(el) {
    var opacity = 1;
    el.style.opacity = 1;
    el.style.filter = '';
  
    var last = +new Date();
    var tick = function() {
      opacity -= (new Date() - last) / 400;
      el.style.opacity = opacity;
      el.style.filter = ("alpha(opacity=" + 100 * opacity) | (0 + ")");
  
      last = +new Date();
  
      if (opacity > 0) {
        (window.requestAnimationFrame && requestAnimationFrame(tick)) ||
          setTimeout(tick, 16);
      }
    };
    tick();
  };
  
  var getCookie = function(name) {
    var allCookies = document.cookie;
    var pos = allCookies.indexOf(name);
    if (pos != -1) {
      var start = pos + name.length + 1;
      var end = allCookies.indexOf(";", start);
      if (end == -1) end = allCookies.length;
      var value = allCookies.substring(start, end);
      value = decodeURIComponent(value);
      return value;
    } else {
      // console.log("no cookie found");
    }
  };
  
  var koekjesBar = function() {
    var barEl = document.getElementById("cookie-bar");
    var COOKIE_NAME = "eu.openimages.cookieallow";
    var koekje = getCookie(COOKIE_NAME);
  
    if (koekje === undefined && barEl) {
      var closeEl = barEl.querySelectorAll(".cookies-textread")[0];
      if (closeEl) {
        closeEl.addEventListener("click", function() {
          setCookie(COOKIE_NAME, "TEXTREAD_COOKIES", 365 * 10);
          fadeOut(barEl);
        });
      }
    } else {
      // console.log("cookie found");
      if (barEl) {
        barEl.parentNode.removeChild(barEl);
      }
    }
  };
  
  ready(koekjesBar);
  