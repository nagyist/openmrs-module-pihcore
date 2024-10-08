package org.openmrs.module.pihcore.identifier.liberia;

import org.openmrs.Location;
import org.openmrs.api.context.Context;
import org.openmrs.module.idgen.AutoGenerationOption;
import org.openmrs.module.idgen.SequentialIdentifierGenerator;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.pihcore.LiberiaConfigConstants;
import org.openmrs.module.pihcore.config.Config;

public class ConfigureLiberiaIdGenerators {


    public static final String LIBERIA_PRIMARY_IDENTIFIER_SOURCE_UUID = "06817a00-f405-11e4-b939-0800200c9a66";

    public static void configureGenerators(IdentifierSourceService identifierSourceService, Config config) {

        Location identifierLocation = getIdentifierLocation(config);

        SequentialIdentifierGenerator liberiaPrimaryIdentifierSource = (SequentialIdentifierGenerator) identifierSourceService.getIdentifierSourceByUuid(LIBERIA_PRIMARY_IDENTIFIER_SOURCE_UUID);
        if (liberiaPrimaryIdentifierSource == null) {
            liberiaPrimaryIdentifierSource = new SequentialIdentifierGenerator();
        }

        liberiaPrimaryIdentifierSource.setName("Liberia Primary Identifier Source");
        liberiaPrimaryIdentifierSource.setDescription("Primary Identifier Generator for Liberia");
        liberiaPrimaryIdentifierSource.setIdentifierType(Context.getPatientService().getPatientIdentifierTypeByUuid(LiberiaConfigConstants.PATIENTIDENTIFIERTYPE_LIBERIAEMRID_UUID));
        liberiaPrimaryIdentifierSource.setPrefix(config.getPrimaryIdentifierPrefix());
        liberiaPrimaryIdentifierSource.setMinLength(7 + liberiaPrimaryIdentifierSource.getPrefix().length());
        liberiaPrimaryIdentifierSource.setMaxLength(8 + liberiaPrimaryIdentifierSource.getPrefix().length());
        liberiaPrimaryIdentifierSource.setBaseCharacterSet("0123456789");
        if (config.getSite().equalsIgnoreCase("HARPER")) {
            liberiaPrimaryIdentifierSource.setFirstIdentifierBase("0100000");
        } else {
            liberiaPrimaryIdentifierSource.setFirstIdentifierBase("0000001");
        }
        liberiaPrimaryIdentifierSource.setUuid(LIBERIA_PRIMARY_IDENTIFIER_SOURCE_UUID);

        identifierSourceService.saveIdentifierSource(liberiaPrimaryIdentifierSource);

        AutoGenerationOption autoGenerationOption = identifierSourceService.getAutoGenerationOption(liberiaPrimaryIdentifierSource
                .getIdentifierType());

        if (autoGenerationOption == null) {
            autoGenerationOption = new AutoGenerationOption();
        }

        autoGenerationOption.setIdentifierType(Context.getPatientService().getPatientIdentifierTypeByUuid(LiberiaConfigConstants.PATIENTIDENTIFIERTYPE_LIBERIAEMRID_UUID));
        autoGenerationOption.setSource(liberiaPrimaryIdentifierSource);
        autoGenerationOption.setAutomaticGenerationEnabled(true);
        autoGenerationOption.setManualEntryEnabled(false);
        autoGenerationOption.setLocation(identifierLocation);

        identifierSourceService.saveAutoGenerationOption(autoGenerationOption);
    }

    private static Location getIdentifierLocation(Config config) {
        String healthFacilityUuid = "5af1ffcd-5178-11ea-a500-645d86728797";
        return Context.getLocationService().getLocationByUuid(healthFacilityUuid);
    }

}
