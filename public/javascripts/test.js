var angularModule = angular.module('ngAppDemo', []);
angularModule.controller('ngAppDemoController',
	function($scope) {
		$scope.a = 1;
		$scope.b = 2;
	}
);