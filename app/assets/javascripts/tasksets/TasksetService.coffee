class TasksetService

  @headers = {'Accept': 'application/json', 'Content-Type': 'application/json'}
  @defaultConfig = {headers: @headers}

  constructor: (@$log, @$http, @$q) ->
    @$log.debug "constructing TasksetService"

  listTasksets: () ->
    @$log.debug "listTasksets()"
    deferred = @$q.defer()

    @$http.get("/tasksets")
    .success((data, status, headers) =>
      @$log.info("Successfully listed Tasksets - status #{status}")
      deferred.resolve(data)
    )
    .error((data, status, headers) =>
      @$log.error("Failed to list Tasksets - status #{status}")
      deferred.reject(data)
    )
    deferred.promise

  createTaskset: (taskset) ->
    @$log.debug "taskset #{angular.toJson(taskset, true)}"
    deferred = @$q.defer()

    @$http.post('/tasksets', taskset)
    .success((data, status, headers) =>
      @$log.info("Successfully created Taskset - status #{status}")
      deferred.resolve(data)
    )
    .error((data, status, headers) =>
      @$log.error("Failed to create taskset - status #{status}")
      deferred.reject(data)
    )
    deferred.promise

  updateTaskset: (firstName, lastName, taskset) ->
    @$log.debug "updateTaskset #{angular.toJson(taskset, true)}"
    deferred = @$q.defer()

    @$http.put("/taskset/#{firstName}/#{lastName}", taskset)
    .success((data, status, headers) =>
      @$log.info("Successfully updated Taskset - status #{status}")
      deferred.resolve(data)
    )
    .error((data, status, header) =>
      @$log.error("Failed to update taskset - status #{status}")
      deferred.reject(data)
    )
    deferred.promise

servicesModule.service('TasksetService', ['$log', '$http', '$q',
  TasksetService])