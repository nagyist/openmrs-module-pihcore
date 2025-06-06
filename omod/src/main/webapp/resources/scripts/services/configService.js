angular.module('configService', ['ngResource'])

    .factory('ConfigResource', [ "$resource", function($resource) {
        return $resource("/" + OPENMRS_CONTEXT_PATH + "/module/pihcore/config.json", {
        },{
            query: { method:'GET', isArray:false, cache:true }
        });
    }])

    .factory('ExtensionResource', [ "$resource", function($resource) {
        return $resource("/" + OPENMRS_CONTEXT_PATH + "/ws/rest/v1/coreapps/extensions", {
        },{
            query: { method:'GET', isArray:false, cache:true }
        });
    }])

    .factory('ConfigService', [ "ConfigResource", function(ConfigResource) {
        return {
            // returns a promise
            getConfig: function() {
                return ConfigResource.query().
                    $promise.then(function(response) {
                        return response;
                    });
            }
        }
    }])

    .factory('CoreappsService', [ "ExtensionResource", function(ExtensionResource, AppResource) {
        return {
            // returns a promise
            getUserExtensionsFor: function(extensionPoint, patient, visit) {
                // TODO handle multiple pages
                var patientUuid = patient.uuid || patient;
                var visitUuid = visit.uuid || visit;
                return ExtensionResource.query({
                    extensionPoint: extensionPoint,
                    patient: patientUuid,
                    visit: visitUuid
                }).$promise.then(function(response) {
                    return response.extensions;
                });
            }
        }
    }]);
