var angularModule = angular.module('ngAppHackathon', []);
angularModule.controller('ngAppHackathonController', ['$scope', '$http',
	function($scope, $http) {
		
		$scope.reducedEvents = [];
		
		$scope.getReducedEvents = function() {
			$http.get('stuff/test').success(function (data) {
				$scope.reducedEvents = data.data;
			})
		}
}]);