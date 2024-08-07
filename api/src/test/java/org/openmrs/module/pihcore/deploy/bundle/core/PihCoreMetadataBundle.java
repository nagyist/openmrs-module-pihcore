package org.openmrs.module.pihcore.deploy.bundle.core;

import org.openmrs.module.metadatadeploy.bundle.AbstractMetadataBundle;
import org.openmrs.module.metadatadeploy.bundle.Requires;
import org.openmrs.module.pihcore.deploy.bundle.core.concept.AdministrativeConcepts;
import org.openmrs.module.pihcore.deploy.bundle.core.concept.InsuranceConcepts;
import org.openmrs.module.pihcore.deploy.bundle.core.concept.SocioEconomicConcepts;
import org.springframework.stereotype.Component;

/**
 * Consolidates all core metadata into a single required set
 */

@Component
@Requires({
        AdministrativeConcepts.class,
        SocioEconomicConcepts.class,
        InsuranceConcepts.class
        })
public class PihCoreMetadataBundle extends AbstractMetadataBundle {
    @Override
    public void install() throws Exception {

    }
}
