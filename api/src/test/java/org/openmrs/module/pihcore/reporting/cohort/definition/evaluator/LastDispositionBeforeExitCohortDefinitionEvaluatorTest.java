/*
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.pihcore.reporting.cohort.definition.evaluator;

import org.junit.jupiter.api.Test;
import org.openmrs.Concept;
import org.openmrs.Location;
import org.openmrs.api.ConceptService;
import org.openmrs.api.LocationService;
import org.openmrs.module.pihcore.reporting.BaseInpatientReportTest;
import org.openmrs.module.pihcore.reporting.cohort.definition.LastDispositionBeforeExitCohortDefinition;
import org.openmrs.module.reporting.cohort.EvaluatedCohort;
import org.openmrs.module.reporting.cohort.definition.service.CohortDefinitionService;
import org.openmrs.module.reporting.common.DateUtil;
import org.openmrs.module.reporting.evaluation.EvaluationContext;
import org.openmrs.test.SkipBaseSetup;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.openmrs.module.pihcore.reporting.ReportingMatchers.isCohortWithExactlyIds;

@SkipBaseSetup
public class LastDispositionBeforeExitCohortDefinitionEvaluatorTest extends BaseInpatientReportTest {

    @Autowired
    ConceptService conceptService;

    @Autowired
    LocationService locationService;

    @Autowired
    CohortDefinitionService cohortDefinitionService;

    @Test
    public void testEvaluate() throws Exception {
        Location mensInternalMedicine = locationService.getLocation("Sal Gason");
        Date startDate = DateUtil.parseDate("2013-10-03 00:00:00", "yyyy-MM-dd HH:mm:ss");
        Date endDate = DateUtil.parseDate("2013-10-03 23:59:59", "yyyy-MM-dd HH:mm:ss");

        Concept discharged = conceptService.getConceptByMapping("DISCHARGED", "PIH");

        LastDispositionBeforeExitCohortDefinition definition = new LastDispositionBeforeExitCohortDefinition();
        definition.setExitFromWard(mensInternalMedicine);
        definition.setExitOnOrAfter(startDate);
        definition.setExitOnOrBefore(endDate);
        definition.addDisposition(discharged);

        EvaluatedCohort result = cohortDefinitionService.evaluate(definition, new EvaluationContext());
        assertThat(result, isCohortWithExactlyIds(patient4.getId()));
    }

}
