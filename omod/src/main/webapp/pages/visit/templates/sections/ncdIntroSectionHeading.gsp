<span class="encounter-name title encounter-span">
    <i class="{{ section.icon }}"></i>  {{ section.label | translate }}
    <i class="icon-exclamation-sign highlight" ng-show="doesNotHaveExistingObs && !hideIncompleteAlert"></i>
</span> <!-- encounter-type class added for smoke tests -->

<span class="obs-span">
    <!-- Show type of visit -->
    <span ng-repeat="obs in encounter.obs | byConcept:Concepts.reasonForNCDvisit">
        {{ obs |  obs:"value" }}{{ \$last ? "" : "," }}
    </span>

    <!-- add a hyphen between reason for visit and visit type -->
    <span ng-show="(encounter.obs | byConcept:Concepts.reasonForNCDvisit).length > 0 && (encounter.obs | byConcept:Concepts.ncdConditionSet).length > 0" >--&nbsp;</span>

    <!-- For SL with obsgroup-->
    <span ng-repeat="obs in encounter.obs | byConcept:Concepts.ncdConditionSet ">
        {{ (obs | groupMember:Concepts.ncdCategory).value | omrsDisplay  }}{{ \$last ? "" : "," }}
    </span>
</span>

<span ng-show="showEncounterDetails" ng-include="'templates/showEncounterDetails.page'" />