// Moves focus to the error summary after a failed submit (WCAG 3.3.1).
document.addEventListener("DOMContentLoaded", function () {
  var summary = document.getElementById("error-summary");
  if (summary) {
    summary.focus();
  }
});
