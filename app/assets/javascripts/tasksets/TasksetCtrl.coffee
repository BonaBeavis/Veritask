class TasksetCtrl

  constructor: (@$log, @TasksetService) ->
    @$log.debug "constructing TasksetController"
    @tasksets = []
    @getAllTasksets()

  getAllTasksets: () ->
    @$log.debug "getAllTasksets()"

    @TasksetService.listTasksets()
    .then(
      (data) =>
        @$log.debug "Promise returned #{data.length} Tasksets"
        @tasksets = data
    ,
      (error) =>
        @$log.error "Unable to get Tasksets: #{error}"
    )

controllersModule.controller('TasksetCtrl', ['$log', 'TasksetService',
  TasksetCtrl])