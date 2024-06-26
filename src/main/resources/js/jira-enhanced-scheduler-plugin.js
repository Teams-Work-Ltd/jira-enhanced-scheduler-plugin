(function ($) {
  // form the URL
  var url = AJS.contextPath() + "/rest/jes/1.0/scheduler";

  // Function to populate the form with configuration data
  function populateForm(config) {
    $("#extraThreadsToConfigure").val(config.extraThreadsToConfigure);
    $("#extraThreadsRunning").text(config.extraThreadsRunning);
    $("#threadGroupName").text(config.threadGroupName);
    $("#extraThreadGroupStarted").text(config.extraThreadGroupStarted);
    $("#defaultThreadGroup").text(config.defaultThreadGroup);

    // Visual cues for extra thread group started
    var isStarted = config.extraThreadGroupStarted;
    if (isStarted) {
      $("#extraThreadGroupStarted").html('<span class="tick"></span>');
    } else {
      $("#extraThreadGroupStarted").html('<span class="cross"></span>');
    }

    // Visual cues for scheduler running.
    if (config.schedulerRunning) {
      $("#toggleScheduler").prop("checked", true);
    } else {
      $("#toggleScheduler").prop("checked", false);
    }

    // Visual cues for scheduler reconfigured.
    if (config.schedulerReconfigured) {
      $("#schedulerReconfigured").html('<span class="tick"></span>');
      $("#reconfigure-scheduler").removeClass("aui-button-primary");
      $("#start-extra-threads").addClass("aui-button-primary");
    } else {
      $("#schedulerReconfigured").html('<span class="cross"></span>'); // Show cross
    }

  }

  // Function to fetch configuration data and populate the form
  function fetchAndPopulateForm() {
    $.ajax({
      url: url,
      dataType: "json"
    }).done(function(config) {
      populateForm(config);
    });
  }

  $(function() {
    fetchAndPopulateForm();

    // Function to reconfigure the scheduler.
    function reconfigureScheduler() {
      $.ajax({
        url: url,
        type: "POST",
        contentType: "application/json",
        data: $("#extraThreadsToConfigure").val(),
        processData: false
      }).done(function() {
        $("#configuration-errors").removeClass("active-errors");
        $("#configuration-errors").addClass("hidden-errors");
        fetchAndPopulateForm();
        $("#configuration-success").fadeIn().delay(5000).fadeOut();
      }).fail(function(jqXHR, textStatus, errorThrown) {
        var errorMessage = jqXHR.responseJSON.message;
        $("#extraThreadsToConfigureError").text(errorMessage);
        $("#configuration-errors").removeClass("hidden-errors");
        $("#configuration-errors").addClass("active-errors");
        fetchAndPopulateForm();
      });
    }

    // Event listener for form submission to reconfigure scheduler.
    $("#configure-threads-form").on("submit", function(e) {
      e.preventDefault();
      reconfigureScheduler();
    });

    // Function to start new threads in the scheduler.
    function startNewThreadGroup() {
      $.ajax({
        url: url + "/startWithConfiguration",
        type: "POST",
        contentType: "application/json",
        processData: false
      }).done(function() {
        fetchAndPopulateForm();
        $("#configuration-errors").removeClass("active-errors");
        $("#configuration-errors").addClass("hidden-errors");
        $("#thread-start-success").fadeIn().delay(5000).fadeOut();
      }).fail(function(jqXHR, textStatus, errorThrown) {
        var errorMessage = jqXHR.responseJSON.message;
        $("#extraThreadsToConfigureError").text(errorMessage);
        $("#configuration-errors").removeClass("hidden-errors");
        $("#configuration-errors").addClass("active-errors");
        fetchAndPopulateForm();
      });
    }

    // Event listener for form submission to reconfigure scheduler.
    $("#start-extra-threads").on("click", function(e) {
      e.preventDefault();
      startNewThreadGroup();
    });

    // Function to toggle the scheduler.
    function toggleScheduler() {
      var isChecked = $("#toggleScheduler").prop("checked");

      // Ask for confirmation
      var confirmationMessage = isChecked ? "Are you sure you want to start the scheduler?" : "Are you sure you want to pause the scheduler?";
      if (!confirm(confirmationMessage)) {
        $("#toggleScheduler").prop("checked", !isChecked);
        return;
      }

      var ajaxUrl = isChecked ? url + "/start" : url + "/pause";
      $.ajax({
        url: ajaxUrl,
        type: "POST",
        contentType: "application/json",
        processData: false
     }).done(function() {
       fetchAndPopulateForm();
     }).fail(function(jqXHR, textStatus, errorThrown) {
         fetchAndPopulateForm();
     });
    }

    // Event listener for form submission to toggle scheduler.
    $("#toggleScheduler").on("change", function() {
       toggleScheduler();
    });
  });
})(AJS.$ || jQuery);