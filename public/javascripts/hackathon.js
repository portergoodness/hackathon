var angularModule = angular.module('ngAppHackathon', ['ui.bootstrap', 'google-maps']);
angularModule.controller('ngAppHackathonController', ['$scope', '$http',
	function($scope, $http) {

		$scope.searchField = {value: ""};

        $scope.resultsFound = -1;
		
		$scope.reducedEvents = [];
		$scope.trainingSetEvents = {};
        $scope.pageObj = {position: 1, size: 10};
        
        $scope.learnedQuerySpecs = [];
		
		$scope.getReducedEvents = function(elementNumber, pageSize) {
            var url = "stuff";

            var paramCombiner = '?';
            if (angular.isDefined(elementNumber)) {
                url += '?start='+elementNumber + '&rows=' + pageSize;
                paramCombiner = '&'
            }
            if ($scope.searchField.value !== "") {
            	var searchFieldText = $scope.searchField.value.replace(" ","%20");
            	
            	url += paramCombiner+'search='+encodeURIComponent("("+$scope.searchField.value+")")
                paramCombiner = "&";
            }

            if (angular.isDefined($scope.searchField.distance) && $scope.searchField.distance !== "") {
            	url += paramCombiner+'lat='+$scope.searchField.lat;
            	paramCombiner = "&";
            	url += paramCombiner+'long='+$scope.searchField.long;
            	url += paramCombiner+'distance='+$scope.searchField.distance;
            		
            }
//            if (angular.isDefined($scope.searchField.distance) && $scope.searchField.distance !== "") {
//                url += paramCombiner+"fq=%7B!geofilt%20sfield=Location%7D&pt=" + $scope.searchField.lat + "," + $scope.searchField.long +
//                    "&d=" + $scope.searchField.distance;
//            }

			$http.get(url).success(function (data) {

                $scope.resultsFound = data.response.numFound;
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
			
			if ($scope.trainingSetEvents.length === 0) return;
			
			var data = {
					positives: [],
					negatives: []
			};
			
			// We use find here so we can break out of the loop if we find CDF hasn't loaded data yet
			// undefined means we had no problems building data object, ie, find never received a true condition
			var undefinedIfSuccessful = _.find($scope.trainingSetEvents, function(event) {
				if (angular.isUndefined(event.__information)) {
					return true;
				}
				if (event.positive) {
					data.positives.push(event.__information.url);
				} else {
					data.negatives.push(event.__information.url);
				}
				return false;
			});
			
			if (angular.isUndefined(undefinedIfSuccessful)) {
				$http.post('trainingSet', data).success(function(resp) {
					$scope.learnedQuerySpecs.push(resp);
				});
			} else {
				alert("CDF has not returned data yet, cannot transmit training set");
			}
			
		}
		
		
}]);