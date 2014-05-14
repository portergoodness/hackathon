var angularModule = angular.module('ngAppHackathon', []);
angularModule.controller('ngAppHackathonController', ['$scope', '$http',
	function($scope, $http) {
		
		$scope.reducedEvents = [];
		$scope.negativeEvents = {};
		$scope.positiveEvents = {};
		
		$scope.getReducedEvents = function() {
			$http.get('stuff').success(function (data) {
				$scope.reducedEvents = data.data;
			})
		}
		
		$scope.isPositiveEvent = function(event) {
			return angular.isDefined($scope.positiveEvents[event.Id]);
		}
		
		$scope.isNegativeEvent = function(event) {
			return angular.isDefined($scope.negativeEvents[event.Id]);
		}
		
		$scope.addPositiveEvent = function(event) {
			if ($scope.isNegativeEvent(event)) {
				delete $scope.negativeEvents[event.Id];
			}
			if ($scope.isNegativeEvent(event)) {
				delete $scope.positiveEvents[event.Id];
			} else {
				$scope.positiveEvents[event.Id] = event;
			}
		}
		
		$scope.addNegativeEvent = function(event) {
			if ($scope.isPositiveEvent(event)) {
				delete $scope.positiveEvents[event.Id];
			}
			if ($scope.isPositiveEvent(event)) {
				delete $scope.negativeEvents[event.Id];
			} else {
				$scope.negativeEvents[event.Id] = event;
			}
		}
		
		$scope.transmitTrainingSet = function() {
			
//				"positives": ["http://hackathon/cdf/a", "http://hackathon/cdf/b"],
//				"negatives": [...
		}
}]);