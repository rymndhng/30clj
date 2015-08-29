// Excuse my awful javascript
Date.prototype.yyyymmdd = function() {
  var yyyy = this.getFullYear().toString();
  var mm = (this.getMonth()+1).toString(); // getMonth() is zero-based
  var dd  = this.getDate().toString();
  return [yyyy, (mm[1]?mm:"0"+mm[0]), (dd[1]?dd:"0"+dd[0])].join('-'); // padding
};

document.getElementById("reset-date").onclick = function(e) {
  document.getElementById("send-date").value = (new Date()).yyyymmdd();
  return false;
};

Array.prototype.forEach.call(
  document.getElementsByClassName('add-days'),
  function(elem) {
    elem.onclick = function(e) {
      var addDays = parseInt(e.target.value);

      values = document
        .getElementById("send-date")
        .value
        .split('-')
        .map(function(x) { return parseInt(x); });

      values[2] += addDays;

      // yep, months offset at 0
      date = new Date(values[0], values[1] - 1, values[2]);
      document.getElementById("send-date").value = date.yyyymmdd();
      return false;
    };
  }
);
