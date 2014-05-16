var angularModule = angular.module('ngAppHackathon', []);
angularModule.controller('ngAppHackathonController', ['$scope', '$http',
	function($scope, $http) {
	
		$scope.searchField = "";
		
		$scope.reducedEvents = [];
		$scope.negativeEvents = {};
		$scope.positiveEvents = {};
		
		$scope.getReducedEvents = function() {
			$http.get('stuff').success(function (data) {
				
				// this is the data from solr, it only has a limited amount of info
				getPrimaryKey = function(doc) {
					return _.last(doc.PrimaryKey.split("/"));
				}
				$scope.reducedEvents = {};
				_.each(data.response.docs, function(doc) {
					var id = getPrimaryKey(doc);
					doc.Id = id;
					$scope.reducedEvents[id] = doc;
				});
				
				//we look up the acutal records from CDF and merge that data into the solr indexed items.
				//this gives us the benefits of solr (paging that works, search, faceting, plus the real data
					
				var eventIds = _.map($scope.reducedEvents, function(event) {return event.Id});
				var urlConstraint = "ids=" + eventIds.join("&ids=");
				
				$http.get('events?'+urlConstraint).success(function (eventData) {
					_.each(eventData.data, function(cdfData) {
						_.extend($scope.reducedEvents[cdfData.Id], cdfData);
					});
				});
				
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