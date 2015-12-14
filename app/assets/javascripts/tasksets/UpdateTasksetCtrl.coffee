class UpdateTasksetCtrl

  constructor: (@$log, @$location, @$routeParams, @TasksetService) ->
    @$log.debug "constructing UpdateTasksetController"
    @taskset = {}
    @findTaskset()

  updateTaskset: () ->
    @$log.debug "updateTaskset()"
    @taskset.active = true
    @TasksetService.updateTaskset(@$routeParams.firstName, @$routeParams.lastName, @taskset)
    .then(
      (data) =>
        @$log.debug "Promise returned #{data} Taskset"
        @taskset = data
        @$location.path("/")
    ,
      (error) =>
        @$log.error "Unable to update Taskset: #{error}"
    )

  findTaskset: () ->
# route params must be same name as provided in routing url in app.coffee
    firstName = @$routeParams.firstName
    lastName = @$routeParams.lastName
    @$log.debug "findTaskset route params: #{firstName} #{lastName}"

    @TasksetService.listTasksets()
    .then(
      (data) =>
        @$log.debug "Promise returned #{data.length} Tasksets"

        # find a taskset with the name of firstName and lastName
        # as filter returns an array, get the first object in it, and return it
        @taskset = (data.filter (taskset) -> taskset.firstName is firstName and taskset.lastName is lastName)[0]
    ,
      (error) =>
        @$log.error "Unable to get Tasksets: #{error}"
    )

controllersModule.controller('UpdateTasksetCtrl', ['$log', '$location',
  '$routeParams', 'TasksetService', UpdateTasksetCtrl])