class CreateTasksetCtrl

  constructor: (@$log, @$location, @$scope, @Upload, @$timeout, @TasksetService) ->
    @$log.debug "constructing CreateTasksetController"

  uploadLinkset: (file) ->
    file.upload = @Upload.upload(
      url: 'https://angular-file-upload-cors-srv.appspot.com/upload'
      data:
        file: file
        tasksetName: $scope.tasksetName)

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

controllersModule.controller('CreateTasksetCtrl', ['$log', '$location', '$scope', 'Upload', '$timeout'
  'TasksetService', CreateTasksetCtrl])