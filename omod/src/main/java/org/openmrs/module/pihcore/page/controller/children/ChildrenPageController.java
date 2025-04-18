package org.openmrs.module.pihcore.page.controller.children;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterProvider;
import org.openmrs.EncounterType;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonAttributeType;
import org.openmrs.Relationship;
import org.openmrs.RelationshipType;
import org.openmrs.api.ConceptService;
import org.openmrs.api.EncounterService;
import org.openmrs.api.PersonService;
import org.openmrs.api.context.Context;
import org.openmrs.module.appui.UiSessionContext;
import org.openmrs.module.coreapps.CoreAppsProperties;
import org.openmrs.module.emrapi.EmrApiProperties;
import org.openmrs.module.pihcore.PihEmrConfigConstants;
import org.openmrs.module.pihcore.metadata.Metadata;
import org.openmrs.module.reporting.common.ObjectUtil;
import org.openmrs.parameter.EncounterSearchCriteriaBuilder;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.page.PageModel;
import org.springframework.web.bind.annotation.RequestParam;

public class ChildrenPageController {

    private static String NEWBORN_DETAILS_CONCEPT = "1585";
    private static String YES_CONCEPT = "1065";
    private static String NO_CONCEPT = "1066";
    private static String REGISTER_BABY_CONCEPT = "20150";
    private static String BABY_GENDER_CONCEPT_SOURCE = "CIEL";
    private static String BABY_GENDER_CONCEPT = "1587";
    private static String DATE_TIME_OF_BIRTH_CONCEPT_SOURCE = "PIH";
    private static String DATE_TIME_OF_BIRTH_CONCEPT = "15080";

    public void controller(PageModel model, UiUtils ui, UiSessionContext uiSessionContext,
                           @RequestParam("patientId") Patient patient,
                           @RequestParam(value="returnUrl", required=false) String returnUrl,
                           @SpringBean("personService") PersonService personService,
                           @SpringBean("encounterService") EncounterService encounterService,
                           @SpringBean("conceptService") ConceptService conceptService,
                           @SpringBean("coreAppsProperties") CoreAppsProperties coreAppsProperties,
                           @SpringBean EmrApiProperties emrApiProperties
                           ) {


        if (StringUtils.isBlank(returnUrl)) {
            returnUrl = ui.pageLink("coreapps", "clinicianfacing/patient", ObjectUtil.toMap("patientId", patient.getUuid()));
        }
        RelationshipType motherToChildRelationshipType = personService.getRelationshipTypeByUuid(PihEmrConfigConstants.RELATIONSHIPTYPE_MOTHERTOCHILD_UUID);
        List<Relationship> relationships = personService.getRelationships(patient.getPerson(), null, motherToChildRelationshipType);
        Map<String, Person> children= new HashMap<String, Person>();
        for (Relationship relationship : relationships) {
            children.put(relationship.getUuid(), relationship.getPersonB());
        }
        HashMap<String, Patient> childrenOrderedByAge = new LinkedHashMap<String, Patient>();
        if (!children.isEmpty()) {
            List<Map.Entry<String, Person>> list = new LinkedList<Map.Entry<String, Person> >(children.entrySet());
            //sort the list
            Collections.sort(list, new Comparator<Map.Entry<String, Person>>() {
                @Override
                public int compare(Map.Entry<String, Person> o1, Map.Entry<String, Person> o2) {
                    return o2.getValue().getBirthdate().compareTo(o1.getValue().getBirthdate());
                }
            });
            for (Map.Entry<String, Person> person : list) {
                childrenOrderedByAge.put(person.getKey(), Context.getPatientService().getPatientByUuid(person.getValue().getUuid()));
            }

        }
        model.addAttribute("patient", patient);
        model.addAttribute("children", childrenOrderedByAge);
        model.addAttribute("primaryIdentifierType", emrApiProperties.getPrimaryIdentifierType());
        model.addAttribute("dashboardUrl", coreAppsProperties.getDashboardUrl());
        model.addAttribute("returnUrl", returnUrl);

        Set<EncounterType> encTypes = new HashSet<>();
        encTypes.add(encounterService.getEncounterTypeByUuid(PihEmrConfigConstants.ENCOUNTERTYPE_LABOR_DELIVERY_SUMMARY_UUID));
        EncounterSearchCriteriaBuilder searchCriteriaBuilder = new EncounterSearchCriteriaBuilder();
        searchCriteriaBuilder.setPatient(patient);
        searchCriteriaBuilder.setEncounterTypes(encTypes);
        List<Encounter> encounters = encounterService.getEncounters(searchCriteriaBuilder.createEncounterSearchCriteria());
        Concept newbornDetailsConcept = conceptService.getConceptByMapping(NEWBORN_DETAILS_CONCEPT, "CIEL");
        Concept registerBabyConcept = conceptService.getConceptByMapping(REGISTER_BABY_CONCEPT, "PIH");
        Concept yesConcept = conceptService.getConceptByMapping(YES_CONCEPT, "CIEL");
        Concept noConcept = conceptService.getConceptByMapping(NO_CONCEPT, "CIEL");

        Map<String, Date> deliveryEncounterDates = new HashMap<String, Date>();
        List<SimpleObject> unregisteredBabies = new ArrayList<SimpleObject>();
        for (Encounter encounter : encounters) {
            Set<EncounterProvider> activeEncounterProviders = encounter.getActiveEncounterProviders();
            for (EncounterProvider activeEncounterProvider : activeEncounterProviders) {
                activeEncounterProvider.getProvider().getName();
            }
            for (Obs candidate : encounter.getObsAtTopLevel(false)){
                if (candidate.getConcept().equals(newbornDetailsConcept)) {
                    Set<Obs> groupMembers = candidate.getGroupMembers();
                    if ( groupMembers != null ) {
                        for (Obs groupMember : groupMembers) {
                            // search for Labour and Delivery obs that have the "Register patient in EMR" obs set to No (SL-617)
                            if(groupMember.getConcept().equals(registerBabyConcept) && groupMember.getValueCoded().equals(noConcept)) {
                                //find the gender and the birthdate time of the baby
                                String gender = getObsValue(groupMembers, conceptService, BABY_GENDER_CONCEPT_SOURCE, BABY_GENDER_CONCEPT);
                                String birthDatetime = getObsValue(groupMembers, conceptService, DATE_TIME_OF_BIRTH_CONCEPT_SOURCE, DATE_TIME_OF_BIRTH_CONCEPT);
                                unregisteredBabies.add(SimpleObject.create(
                                        "encounterId", encounter.getEncounterId(),
                                        "encounterUuid", encounter.getUuid(),
                                        "patientUuid", patient.getUuid(),
                                        "encounterDatetime", encounter.getEncounterDatetime(),
                                        "provider", encounter.getActiveEncounterProviders().stream().map(p -> p.getProvider().getName()).collect(Collectors.joining(",")),
                                        "birthDatetime", birthDatetime,
                                        "gender", gender,
                                        "registerBabyObs", groupMember.getUuid()
                                ));
                            } else if(groupMember.getConcept().equals(registerBabyConcept)
                                    && groupMember.getValueCoded().equals(yesConcept)
                                    && (groupMember.getComment() != null)
                                    && (groupMember.getComment().length() == 36)) {
                                // the baby was already registered, and the uuid of the baby is stored as a comment to this obs
                                deliveryEncounterDates.put(groupMember.getComment(), encounter.getEncounterDatetime());
                            }
                        }
                    }
                }
            }
        }
        model.addAttribute("unregisteredBabies", unregisteredBabies);
        model.addAttribute("deliveryEncounterDates", deliveryEncounterDates);

        String phoneNumber = null;
        PersonAttributeType phoneNumberAttributeType = personService.getPersonAttributeTypeByUuid(Metadata.getPhoneNumberAttributeType().getUuid());
        if (phoneNumberAttributeType != null ) {
            PersonAttribute attribute = patient.getAttribute(phoneNumberAttributeType);
            if ( attribute != null) {
                phoneNumber = attribute.getValue();
            }
        }
        String patientInfo = patient.getPatientIdentifier().getIdentifier() + " - "
                + patient.getFamilyName() + ", "
                + patient.getGivenName() + ", "
                + patient.getGender() + ", "
                + patient.getBirthdate().toString();
        // pass the mother's info to the baby registration form as a list of property-value in the following format:
        // SECTION.QUESTION.FIELD , this matches the existing format of the registration form
        SimpleObject initialValues = SimpleObject.create(
                "demographics.demographics-name.familyName", patient.getFamilyName(),
                "demographics.demographics-name.givenName", "Baby",
                "demographics.mothersFirstNameLabel.mothersFirstName", patient.getGivenName(),
                "contactInfo.personAddressQuestion.country", (patient.getPersonAddress() != null && patient.getPersonAddress().getCountry() != null) ? patient.getPersonAddress().getCountry() : "",
                "contactInfo.phoneNumberLabel.phoneNumber", phoneNumber,
                "registerRelationships.relationships_mother.relationship_type", motherToChildRelationshipType.getUuid() + "-A",
                "registerRelationships.relationships_mother.other_person_uuid", patient.getUuid(),
                "registerRelationships.relationships_mother.other_person_name", patientInfo,
                "registerRelationships.relationships_mother.mother-field", patientInfo
        );
        model.addAttribute("motherToChildRelationshipType", motherToChildRelationshipType);
        model.addAttribute("initialRegistrationValues", ui.toJson(initialValues));
    }

    private String getObsValue(Set<Obs> groupMembers, ConceptService conceptService, String conceptSource, String conceptId) {
        String obsValue= null;
        Concept obsConcept = conceptService.getConceptByMapping(conceptId, conceptSource);
        if (obsConcept != null ) {
            for (Obs groupMember : groupMembers) {
                if (groupMember.getConcept().equals(obsConcept)) {
                    obsValue = groupMember.getValueAsString(Context.getLocale());
                    break;
                }
            }
        }
        return obsValue;
    }
}
