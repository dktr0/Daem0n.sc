var mainDiv;
var textCells;
var textCell1;
var textCell2;
var textCell3;
var textCell4;
var textCell5;
var text1;
var text2;
var text3;
var text4;
var loudness, centroid, flatness, drone, rhythm, bebop, progress, volatility, intensity, complexity;

$(function() {
  var textCells = document.createElement("div");
  $(textCells).css('display','flex');
  $(textCells).css('flex-direction','column');
  $(textCells).css('margin','5px');
  textCell1 = document.createElement("div");
  textCell2 = document.createElement("div");
  textCell3 = document.createElement("div");
  textCell4 = document.createElement("div");
  textCell5 = document.createElement("div");
  $(textCell1).css('flex-grow','1'); $(textCell1).css('margin','5px'); $(textCell1).css('fontSize','3em'); $(textCell1).css('min-width','100%');
  $(textCell2).css('flex-grow','1'); $(textCell2).css('margin','5px'); $(textCell2).css('fontSize','3em'); $(textCell2).css('min-width','100%');
  $(textCell3).css('flex-grow','1'); $(textCell3).css('margin','5px'); $(textCell3).css('fontSize','3em'); $(textCell3).css('min-width','100%');
  $(textCell4).css('flex-grow','1'); $(textCell4).css('margin','5px'); $(textCell4).css('fontSize','3em'); $(textCell4).css('min-width','100%');
  $(textCell5).css('flex-grow','1'); $(textCell5).css('margin','5px'); $(textCell5).css('fontSize','3em'); $(textCell5).css('min-width','100%');
  $(textCells).append(textCell1,textCell2,textCell3,textCell4,textCell5);
  var mainDiv = document.createElement("div");
  $(mainDiv).append(textCells);
  $(mainDiv).css('height','100%');
  $(mainDiv).css('display','flex');
  $("body").append(mainDiv);
  $("body").css('background-color','black');
  $("body").css('color','lightgreen');
  $("body").css('height','100%');
  $("body").css('width','100%');
  $("body").css('display','flex');
  $(textCell1).text("cell1");
  $(textCell2).text("cell2");
  $(textCell3).text("cell3");
  $(textCell4).text("cell4");
  $(textCell5).text("cell5");
  var x = document.createElement("div");
  $(x).css('fontSize','1em');
  $(x).attr('id','debugDisplay');
  $(mainDiv).append(x);

  debugDisplay("loudness"); $("body").append("&nbsp;&nbsp;&nbsp;<br/>");
  debugDisplay("centroid"); $("body").append("&nbsp;&nbsp;&nbsp;");
  debugDisplay("flatness"); $("body").append("&nbsp;&nbsp;&nbsp;");
  debugDisplay("drone"); $("body").append("&nbsp;&nbsp;&nbsp;");
  debugDisplay("rhythm"); $("body").append("&nbsp;&nbsp;&nbsp;");
  debugDisplay("bebop"); $("body").append("&nbsp;&nbsp;&nbsp;");
  debugDisplay("progress"); $("body").append("&nbsp;&nbsp;&nbsp;");
  debugDisplay("volatility"); $("body").append("&nbsp;&nbsp;&nbsp;");
  debugDisplay("intensity"); $("body").append("&nbsp;&nbsp;&nbsp;");
  debugDisplay("complexity");$("body").append("&nbsp;&nbsp;&nbsp;");
});

function debugDisplay(n) {
  var y = document.createElement("div");
  $(y).css("display","block");
  var x = document.createElement("span");
  $(x).text("---");
  $(x).attr("id",n);
  $(y).append(n);
  $(y).append(x);
  $("#debugDisplay").append(y);
}

function setText1(t) {
  console.log("setText1: " + t);
  text1 = t;
  $(textCell1).text(t);
}

function setText2(t) {
  console.log("setText2: " + t);
  text2 = t;
  $(textCell2).text(t);
}

function setText3(t) {
  console.log("setText3: " + t);
  text3 = t;
  $(textCell3).text(t);
}

function setText4(t) {
  console.log("setText4: " + t);
  text4 = t;
  $(textCell4).text(t);
}

function setText5(t) {
  console.log("setText5: " + t);
  text5 = t;
  $(textCell5).text(t);
}

function setLoudness(x) { loudness = x; $('#loudness').text(x); }
function setCentroid(x) { centroid = x; $('#centroid').text(x); }
function setFlatness(x) { flatness = x; $('#flatness').text(x); }
function setDrone(x) { drone = x; $('#drone').text(x); }
function setRhythm(x) { rhythm = x; $('#rhythm').text(x); }
function setBebop(x) { bebop = x; $('#bebop').text(x); }
function setProgress(x) { progress = x; $('#progress').text(x); }
function setVolatility(x) { volatility = x; $('#volatility').text(x); }
function setIntensity(x) { intensity = x; $('#intensity').text(x); }
function setComplexity(x) { complexity = x; $('#complexity').text(x); }
