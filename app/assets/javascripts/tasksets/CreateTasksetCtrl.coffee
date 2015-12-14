class CreateTasksetCtrl

  constructor: (@$log, @$location, @TasksetService) ->
    @$log.debug "constructing CreateTasksetController"
    @taskset = {}

  createTaskset: () ->
    @$log.debug "createTaskset()"
    @taskset.active = true
    @TasksetService.createTaskset(@taskset)
    .then(
      (data) =>
        @$log.debug "Promise returned #{data} Taskset"
        @taskset = data
        @$location.path("/")
    ,
      (error) =>
        @$log.error "Unable to create Taskset: #{error}"
    )

controllersModule.controller('CreateTasksetCtrl', ['$log', '$location',
  'TasksetService', CreateTasksetCtrl])