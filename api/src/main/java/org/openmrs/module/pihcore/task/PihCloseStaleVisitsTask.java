package org.openmrs.module.pihcore.task;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.StopWatch;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.openmrs.Encounter;
import org.openmrs.Location;
import org.openmrs.LocationTag;
import org.openmrs.Visit;
import org.openmrs.api.LocationService;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.module.emrapi.EmrApiConstants;
import org.openmrs.module.emrapi.adt.AdtService;
import org.openmrs.module.emrapi.disposition.Disposition;
import org.openmrs.module.emrapi.disposition.DispositionType;
import org.openmrs.module.emrapi.visit.VisitDomainWrapper;
import org.openmrs.module.pihcore.PihEmrConfigConstants;
import org.openmrs.module.pihcore.config.Config;
import org.openmrs.module.queue.api.QueueEntryService;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.util.OpenmrsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Our own custom implementation to close stale visits--this does logic similar to those found in AdtService
 * closeInactiveVisits(), but contains our special logic for the ED use case
 */
public class PihCloseStaleVisitsTask implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private Config config;

    private static int REGULAR_VISIT_EXPIRE_TIME_IN_HOURS = 12;
    private static int ED_VISIT_EXPIRE_TIME_IN_HOURS = 168; // 7days -- All ED visits stay open for at least 7 days
    private static int ED_VISIT_EXPIRE_VERY_OLD_TIME_IN_HOURS = 720; // 30 days (UHM-3009)

    private static final List<String> ED_LOCATION_UUIDS = Arrays.asList(
            "f3a5586e-f06c-4dfb-96b0-6f3451a35e90",  // Ijans (Emergency)
            "afa09010-43b6-4f19-89e0-58d09941bcbd",  // Ijans Resepsyon (Emergency department reception)
            "d65eb8cf-d781-4ea8-9d9a-2b3e03c6074c");  // "Ijans | Sante Fanm"  (Women's triage)

    private static final String ENCOUNTER_TYPE_ED_TRIAGE_UUID = PihEmrConfigConstants.ENCOUNTERTYPE_EMERGENCY_TRIAGE_UUID;

    private static boolean isExecuting = false;

    @Override
    public void run() {
        if (isExecuting) {
            log.info(getClass() + " is still executing, not running again");
            return;
        }
        isExecuting = true;
        try {
            log.info("Executing " + getClass());
            StopWatch sw = new StopWatch();
            sw.start();
            if (config == null) {
                config = Context.getRegisteredComponent("config", Config.class);
            }

            AdtService adtService = Context.getService(AdtService.class);
            VisitService visitService = Context.getVisitService();
            LocationService locationService = Context.getLocationService();

            LocationTag visitLocationTag = locationService.getLocationTagByName(EmrApiConstants.LOCATION_TAG_SUPPORTS_VISITS);
            List<Location> locations = locationService.getLocationsByTag(visitLocationTag);

            if (locations == null || locations.size() == 0) {
                log.error("Unable to close stale visits, no locations with Visit Location tag");
                return;
            }

            List<Visit> openVisits = visitService.getVisits(null, null, locations, null, null, null, null, null, null, false, false);
            log.info("Found " + openVisits.size() + " open visits");
            int numVisitsClosed = 0;
            for (Visit visit : openVisits) {

                VisitDomainWrapper wrappedVisit = adtService.wrap(visit);
                Boolean changedOrUpdatedRecently = changedOrUpdatedRecently(visit, REGULAR_VISIT_EXPIRE_TIME_IN_HOURS);

                Disposition mostRecentDisposition = wrappedVisit.getMostRecentDisposition();
                Long hoursSinceLastEncounter = wrappedVisit.getMostRecentEncounter() != null ?
                        new Duration(new DateTime(wrappedVisit.getMostRecentEncounter().getEncounterDatetime()), new DateTime()).getStandardHours()
                        : null;

                // **this logic is intentionally verbose, since it can be difficult to follow**
                Boolean closeVisit = false;

                // if the patient has been discharged, close visit
                if (wrappedVisit.hasBeenDischarged()) {
                    closeVisit = true;
                }
                // if the patient has been admitted, or is awaiting admission, don't close the visit
                else if (wrappedVisit.isAdmitted() || wrappedVisit.isAwaitingAdmission()) {
                    closeVisit = false;
                }
                // otherwise, branch based on whether this is a "regular" visits, or a "ED Visit", with our special logic
                else if (isEDVisit(wrappedVisit) && (config != null) && config.isHaiti()) {
                    // special logic for Haiti ED visits:
                    // if the most recent disposition is "discharge" and there are no encounters in the last 12 hours, and visit hasn't been updated in the last 12 hours, close
                    if (mostRecentDisposition != null && mostRecentDisposition.getType() != null && mostRecentDisposition.getType().equals(DispositionType.DISCHARGE) &&
                            (hoursSinceLastEncounter == null || hoursSinceLastEncounter > REGULAR_VISIT_EXPIRE_TIME_IN_HOURS) && !changedOrUpdatedRecently) {
                        closeVisit = true;
                    }
                    // if the most recent disposition isn't one that "keeps a visit open" (for example "ED Observation" and "Still hospitalized") and there are no encounters in the last 7 days, and visit hasn't been updated in the last 12 hours, close
                    else if ((mostRecentDisposition == null || mostRecentDisposition.getKeepsVisitOpen() == null || !mostRecentDisposition.getKeepsVisitOpen()) &&
                            (hoursSinceLastEncounter == null || hoursSinceLastEncounter > ED_VISIT_EXPIRE_TIME_IN_HOURS) && !changedOrUpdatedRecently) {
                        closeVisit = true;
                    }
                    // otherwise,(regardless of any "keeps visit open" disposition) if there are no encounters in the last 30 days, and visit hasn't been updated in the last 12 hours, close
                    else if ((hoursSinceLastEncounter == null || hoursSinceLastEncounter > ED_VISIT_EXPIRE_VERY_OLD_TIME_IN_HOURS) && !changedOrUpdatedRecently) {
                        closeVisit = true;
                    }
                    // otherwise, don't close
                    else {
                        closeVisit = false;
                    }
                } else {
                    // "normal" visits:
                    // if the disposition is one that "keeps a visit open" ("ED Observation" and "Still hospitalized") keep the visit open
                    if (mostRecentDisposition != null && mostRecentDisposition.getKeepsVisitOpen() != null && mostRecentDisposition.getKeepsVisitOpen()) {
                        closeVisit = false;
                    }
                    // otherwise, close the visit if there are no encounter, or no encounters in the last 12 hours, and the visit hasn't been updated in the last 12 hours
                    else if ((hoursSinceLastEncounter == null || hoursSinceLastEncounter > REGULAR_VISIT_EXPIRE_TIME_IN_HOURS) && !changedOrUpdatedRecently) {
                        closeVisit = true;
                    }
                    // otherwise, don't close
                    else {
                        closeVisit = false;
                    }
                }

                if (closeVisit) {
                    try {
                        visit.setStopDatetime(getLatestDateWithinVisit(visit));
                        visitService.saveVisit(visit);
                        numVisitsClosed++;
                    } catch (Exception ex) {
                        log.warn("Failed to close inactive visit " + visit, ex);
                    }
                }
            }
            sw.stop();
            log.info("Getting open visits completed.");
            log.info(getClass() + " Completed in " + sw + " - " + numVisitsClosed + " Visits Closed");
        }
        finally {
            isExecuting = false;
        }
    }

    public Date getLatestDateWithinVisit(Visit visit) {
        Date latestDate = visit.getStartDatetime();
        if (visit.getEncounters() != null) {
            for (Encounter e : visit.getEncounters()) {
                if (BooleanUtils.isNotTrue(e.getVoided())) {
                    if (OpenmrsUtil.compare(e.getEncounterDatetime(), latestDate) > 0) {
                        latestDate = e.getEncounterDatetime();
                    }
                }
            }
        }
        if (visit.getVisitId() != null) {
            QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
            criteria.setVisit(visit);
            for (QueueEntry qe : Context.getService(QueueEntryService.class).getQueueEntries(criteria)) {
                if (OpenmrsUtil.compare(qe.getStartedAt(), latestDate) > 0) {
                    latestDate = qe.getStartedAt();
                }
                if (OpenmrsUtil.compareWithNullAsEarliest(qe.getEndedAt(), latestDate) > 0) {
                    latestDate = qe.getEndedAt();
                }
            }
        }
        return latestDate;
    }

    private boolean changedOrUpdatedRecently(Visit visit, int hours) {
        Date comparisonDate = DateUtils.addHours(new Date(), -hours);
        return visit.getDateCreated().after(comparisonDate) || (visit.getDateChanged() != null && visit.getDateChanged().after(comparisonDate));
    }

    private boolean isEDVisit(VisitDomainWrapper visit) {
        return hasCheckInAtEDLocation(visit) || hasEDTriageEncounter(visit);
    }

    private boolean hasCheckInAtEDLocation(VisitDomainWrapper visit) {

        Encounter mostRecentCheckIn = visit.getMostRecentCheckInEncounter();

        if (mostRecentCheckIn != null && mostRecentCheckIn.getLocation() != null
                && ED_LOCATION_UUIDS.contains(mostRecentCheckIn.getLocation().getUuid())) {
            return true;
        }
        else {
            return false;
        }

    }

    private boolean hasEDTriageEncounter(VisitDomainWrapper visit) {
        if (visit.hasEncounters()) {
            for (Encounter encounter : visit.getSortedEncounters()) {
                if (encounter.getEncounterType().getUuid().equals(ENCOUNTER_TYPE_ED_TRIAGE_UUID)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasNotBeenDischarged(VisitDomainWrapper visit) {
        Disposition mostRecentDisposition = visit.getMostRecentDisposition();
        if (mostRecentDisposition != null && DispositionType.DISCHARGE.equals(mostRecentDisposition.getType())) {
            return false;
        }
        else {
            return true;
        }
    }
    // to allow mocking in tests
    public void setConfig(Config config) {
        this.config = config;
    }

}
