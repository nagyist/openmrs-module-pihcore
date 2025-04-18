package org.openmrs.module.pihcore.identifier.sierraLeone;

import org.openmrs.Location;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.module.idgen.AutoGenerationOption;
import org.openmrs.module.idgen.SequentialIdentifierGenerator;
import org.openmrs.module.idgen.service.IdentifierSourceService;
import org.openmrs.module.pihcore.SierraLeoneConfigConstants;
import org.openmrs.module.pihcore.config.Config;

public class ConfigureSierraLeoneIdGenerators {

    public static final String WELLBODY_PRIMARY_IDENTIFIER_SOURCE_UUID = "a1516200-7427-11e5-a837-0800200c9a66";
    public static final String KGH_ID_IDENTIFIER_SOURCE_UUID = "809b23e3-7162-11eb-8aa6-0242ac110002";

    public static void configurePrimaryIdentifierSource(IdentifierSourceService iss, Config config) {
        if (config.isWellbody()) {
            configurePrimaryIdentifierSource(
                    iss,
                    WELLBODY_PRIMARY_IDENTIFIER_SOURCE_UUID,
                    "Wellbody Primary Identifier Source",
                    "Primary Identifier Generator for Wellbody",
                    SierraLeoneConfigConstants.PATIENTIDENTIFIERTYPE_WELLBODYEMRID_UUID,
                    "WBA",
                    9,
                    9,
                    "1234567890",
                    "100001",
                    "b6733150-7426-11e5-a837-0800200c9a66"  // Wellbody Clinic
                    );
        }
        else if (config.isKgh()) {

            // First off, we need to override the Sequential Identifier Generator Processor with a custom version
            KghIdGeneratorProcessor processor = Context.getRegisteredComponents(KghIdGeneratorProcessor.class).get(0);
            iss.registerProcessor(SequentialIdentifierGenerator.class, processor);

            configurePrimaryIdentifierSource(
                    iss,
                    KGH_ID_IDENTIFIER_SOURCE_UUID,
                    "KGH Primary Identifier Source",
                    "Primary Identifier Generator for KGH",
                    SierraLeoneConfigConstants.PATIENTIDENTIFIERTYPE_KGHEMRID_UUID,
                    "'KGH'yyMM",
                    11,
                    12,
                    "0123456789",
                    "0001",
                    "074b2ab0-716a-11eb-8aa6-0242ac110002"  // KGH
            );
        }
        else {
            throw new IllegalStateException("Unknown configuration site found.  Expecteed one of 'WELLBODY' or 'KGH'");
        }
    }

    public static void configurePrimaryIdentifierSource(IdentifierSourceService iss, String uuid,
            String name, String description, String identifierTypeUuid, String prefix,
            int minLength, int maxLength, String baseChars, String firstBase, String locationUuid) {

        // Metadata
        PatientIdentifierType identifierType = Context.getPatientService().getPatientIdentifierTypeByUuid(identifierTypeUuid);
        Location identifierLocation = Context.getLocationService().getLocationByUuid(locationUuid);

        // Generator
        SequentialIdentifierGenerator generator = (SequentialIdentifierGenerator) iss.getIdentifierSourceByUuid(uuid);
        if (generator == null) {
            generator = new SequentialIdentifierGenerator();
        }
        generator.setUuid(uuid);
        generator.setName(name);
        generator.setDescription(description);
        generator.setIdentifierType(identifierType);
        generator.setPrefix(prefix);
        generator.setMinLength(minLength);
        generator.setMaxLength(maxLength);
        generator.setBaseCharacterSet(baseChars);
        generator.setFirstIdentifierBase(firstBase);
        iss.saveIdentifierSource(generator);

        // Autogeneration Option
        AutoGenerationOption autoGenerationOption = iss.getAutoGenerationOption(identifierType, identifierLocation);
        if (autoGenerationOption == null) {
            autoGenerationOption = new AutoGenerationOption();
        }
        autoGenerationOption.setSource(generator);
        autoGenerationOption.setIdentifierType(identifierType);
        autoGenerationOption.setAutomaticGenerationEnabled(true);
        autoGenerationOption.setManualEntryEnabled(false);
        autoGenerationOption.setLocation(identifierLocation);
        iss.saveAutoGenerationOption(autoGenerationOption);
    }
}
