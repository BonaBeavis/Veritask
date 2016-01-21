
dependencies = [
    'ngRoute',
    'ui.bootstrap',
    'ngFileUpload',
    'myApp.filters',
    'myApp.services',
    'myApp.controllers',
    'myApp.directives',
    'myApp.common',
    'myApp.routeConfig'
]

app = angular.module('myApp', dependencies)

angular.module('myApp.routeConfig', ['ngRoute'])
    .config(['$routeProvider', ($routeProvider) ->
        $routeProvider
            .when('/', {
                templateUrl: '/assets/partials/viewTasksets.html'
            })
            .when('/taskset/create', {
                templateUrl: '/assets/partials/createTaskset.html'
            })
            .when('/users/edit/:firstName/:lastName', {
                templateUrl: '/assets/partials/update.html'
            })
            .when('/tasksets', {
                templateUrl: '/assets/partials/tasksets.html'
            })
            .otherwise({redirectTo: '/'})
    ])
    .config(['$locationProvider', ($locationProvider) ->
        $locationProvider.html5Mode({
            enabled: true,
            requireBase: false
        })
    ])

@commonModule = angular.module('myApp.common', [])
@controllersModule = angular.module('myApp.controllers', ['ngFileUpload'])
@servicesModule = angular.module('myApp.services', [])
@modelsModule = angular.module('myApp.models', [])
@directivesModule = angular.module('myApp.directives', [])
@filtersModule = angular.module('myApp.filters', [])