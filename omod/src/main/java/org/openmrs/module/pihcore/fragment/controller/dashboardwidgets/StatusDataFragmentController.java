package org.openmrs.module.pihcore.fragment.controller.dashboardwidgets;

import org.codehaus.jackson.JsonNode;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.module.appframework.domain.AppDescriptor;
import org.openmrs.module.emrapi.patient.PatientDomainWrapper;
import org.openmrs.module.pihcore.status.StatusData;
import org.openmrs.module.pihcore.status.StatusDataEvaluator;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.ui.framework.annotation.FragmentParam;
import org.openmrs.ui.framework.annotation.InjectBeans;
import org.openmrs.ui.framework.annotation.SpringBean;
import org.openmrs.ui.framework.fragment.FragmentConfiguration;
import org.openmrs.ui.framework.fragment.FragmentModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class StatusDataFragmentController {

	private final Logger log = LoggerFactory.getLogger(getClass());

	public void controller(@SpringBean("patientService") PatientService patientService,
						   @SpringBean("statusDataEvaluator") StatusDataEvaluator statusDataEvaluator,
						   @InjectBeans PatientDomainWrapper patientWrapper,
						   @FragmentParam("app") AppDescriptor app,
						   UiUtils ui,
						   FragmentConfiguration config,
						   FragmentModel model) {

		Object patientConfig = config.get("patient");
		if (patientConfig == null ) {
			patientConfig = config.get("patientId");
		}
		Patient patient = null;
		if (patientConfig != null) {
			if (patientConfig instanceof Patient) {
				patient = (Patient) patientConfig;
			}
			else if (patientConfig instanceof PatientDomainWrapper) {
				patient = ((PatientDomainWrapper) patientConfig).getPatient();
			}
			else if (patientConfig instanceof Integer) {
				patient = patientService.getPatient((Integer)patientConfig);
			}
			else if (patientConfig instanceof String) {
				patient = patientService.getPatientByUuid((String)patientConfig);
			}
		}
		if (patient == null) {
			throw new IllegalArgumentException("No patient found in the status data fragment.  Please pass patient into the configuration");
		}

		model.put("app", app);

		// Evaluate Status Data based on configFile specified in app configuration
		JsonNode configFileNode = app.getConfig().get("configFile");
		if (configFileNode == null || configFileNode.asText() == null) {
			throw new IllegalStateException("You must supply a configFile configuration to the statusData fragment");
		}
		List<StatusData> statusData = statusDataEvaluator.evaluate(patient, configFileNode.asText());
		statusData.removeIf(statusData1 -> !statusData1.isEnabled());
		model.put("statusData", statusData);
	}
}
