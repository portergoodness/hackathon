var angularModule = angular.module('ngAppHackathon', ['ui.bootstrap']);
angularModule.controller('ngAppHackathonController', ['$scope', '$http',
	function($scope, $http) {
	
		$scope.searchField = "";
		
		$scope.reducedEvents = [];
		$scope.trainingSetEvents = {};
        $scope.pageObj = {position: 1, size: 10};
		
		$scope.getReducedEvents = function(elementNumber, pageSize) {
            var url = "stuff";

            var paramCombiner = '?';
            if (angular.isDefined(elementNumber)) {
                url += '?start='+elementNumber + '&rows=' + pageSize;
                paramCombiner = '&'
            }
            if ($scope.searchField !== "") {
            	url += paramCombiner+'search='+encodeURIComponent($scope.searchField)
            }

			$http.get(url).success(function (data) {
				
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

        $scope.pageFunc = function(pageDirection) {
            if ($scope.pageObj.position + (pageDirection * $scope.pageObj.size)< 1 )
                return;

            $scope.pageObj.position+= (pageDirection * $scope.pageObj.size);
            $scope.getReducedEvents($scope.pageObj.position, $scope.pageObj.size);
        }
		
		$scope.isPositiveEvent = function(event) {
			return angular.isDefined($scope.trainingSetEvents[event.Id]) && $scope.trainingSetEvents[event.Id].positive;
		}
		
		$scope.isNegativeEvent = function(event) {
			return angular.isDefined($scope.trainingSetEvents[event.Id]) && !$scope.trainingSetEvents[event.Id].positive;
		}
		
		$scope.makeEvent = function(jsEvent, event, isPositive) {
            if (!jsEvent.target.checked) {
                delete $scope.trainingSetEvents[event.Id];
                return;
            }

            if (isPositive) {
                event.positive = true;
            }
			else {
                event.positive = false;
            }
            $scope.trainingSetEvents[event.Id] = event;
		}

		$scope.transmitTrainingSet = function() {
			
//				"positives": ["http://hackathon/cdf/a", "http://hackathon/cdf/b"],
//				"negatives": [...
		}
		
		
}]);