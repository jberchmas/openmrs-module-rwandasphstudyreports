package org.openmrs.module.rwandasphstudyreports.reports;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmrs.Concept;
import org.openmrs.EncounterType;
import org.openmrs.Program;
import org.openmrs.api.context.Context;
import org.openmrs.module.reporting.cohort.definition.CodedObsCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.cohort.definition.InverseCohortDefinition;
import org.openmrs.module.reporting.cohort.definition.SqlCohortDefinition;
import org.openmrs.module.reporting.common.SortCriteria;
import org.openmrs.module.reporting.common.SortCriteria.SortDirection;
import org.openmrs.module.reporting.evaluation.parameter.ParameterizableUtil;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.rowperpatientreports.dataset.definition.RowPerPatientDataSetDefinition;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateDiff;
import org.openmrs.module.rowperpatientreports.patientdata.definition.PatientAttribute;
import org.openmrs.module.rowperpatientreports.patientdata.definition.DateDiff.DateDiffType;
import org.openmrs.module.rwandareports.reporting.SetupReport;
import org.openmrs.module.rwandasphstudyreports.Cohorts;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertiesManagement;
import org.openmrs.module.rwandasphstudyreports.GlobalPropertyConstants;
import org.openmrs.module.rwandasphstudyreports.Helper;
import org.openmrs.module.rwandasphstudyreports.RowPerPatientColumns;

public class HIVPositivePatientsDelayInLinkageToCareReport implements SetupReport {
	GlobalPropertiesManagement gp = new GlobalPropertiesManagement();

	private Program hivProgram;

	private Concept scheduledVisit;

	private List<EncounterType> encounterTypes;

	private Concept cd4Count;

	private Concept viralLoad;

	private EncounterType adultFollowUpEncounterType;

	private Concept hivStatus;

	private Concept telephone;

	private Concept telephone2;

	private Concept guardianTelephone;

	private Concept contactTelephone;
	BaseSPHReportConfig config = new BaseSPHReportConfig();

	@Override
	public void setup() throws Exception {
		if("true".equals(Context.getAdministrationService().getGlobalProperty(BaseSPHReportConfig.RECREATEREPORTSONACTIVATION)))
			delete();
		setupProperties();
		setupProperties();

		ReportDefinition rd = createReportDefinition();
		config.setupReport(rd, "HIVPositivePatientsDelayInLinkageToCare",
				"HIVPositivePatientsDelayInLinkageToCare.xls");
	}

	@Override
	public void delete() {
		config.deleteReportDefinition("HIVPositivePatientsDelayInLinkageToCare");
	}

	private ReportDefinition createReportDefinition() {
		ReportDefinition reportDefinition = config.createReportDefinition("HIVPositivePatientsDelayInLinkageToCare");
		createDataSetDefinition(reportDefinition);
		reportDefinition.setUuid(BaseSPHReportConfig.HIVPOSITIVEPATIENTSDELAYINLINKAGETOCAREREPORT);
		reportDefinition.setDescription("HIV Positive Patients Delayed In LinkageToCare");
		reportDefinition.setBaseCohortDefinition(Cohorts.getVCTInclusiveCohortDefinition(), null);
		Helper.saveReportDefinition(reportDefinition);
		
		return reportDefinition;
	}

	private void createDataSetDefinition(ReportDefinition reportDefinition) {
		RowPerPatientDataSetDefinition dataSetDefinition = new RowPerPatientDataSetDefinition();
		DateDiff daysSinceHivPositive = RowPerPatientColumns.getDifferenceSinceLastObservation("DaysSinceHivPositive",
				hivStatus, DateDiffType.DAYS);

		SortCriteria sortCriteria = new SortCriteria();
		Map<String, Object> mappings = new HashMap<String, Object>();
		
		mappings.put("endDate", "${endDate}");
		mappings.put("startDate", "${startDate}");

		sortCriteria.addSortElement("nextRDV", SortDirection.ASC);
		sortCriteria.addSortElement("familyName", SortDirection.ASC);
		sortCriteria.addSortElement("LastVisit Date", SortDirection.DESC);
		dataSetDefinition.setSortCriteria(sortCriteria);

		dataSetDefinition.addParameter(reportDefinition.getParameter("startDate"));
		dataSetDefinition.addParameter(reportDefinition.getParameter("endDate"));
		dataSetDefinition.setName(reportDefinition.getName() + " Data Set");
		
		dataSetDefinition.addColumn(RowPerPatientColumns.getSystemId("patientID"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getFirstNameColumn("givenName"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getFamilyNameColumn("familyName"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getGender("sex"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getDateOfBirth("birth_date", "dd/MMM/yyyy", null),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecentHIVTest("hivTest", "dd/MMM/yyyy"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getDrugOrderForStartOfART("artInitiation", "dd/MMM/yyyy"),
				new HashMap<String, Object>());

		daysSinceHivPositive.addParameter(reportDefinition.getParameter("startDate"));
		daysSinceHivPositive.addParameter(reportDefinition.getParameter("endDate"));
		dataSetDefinition.addColumn(daysSinceHivPositive,
				ParameterizableUtil.createParameterMappings("startDate=${startDate},endDate=${endDate}"));
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("nextRDV", scheduledVisit, "dd/MMM/yyyy"),
				new HashMap<String, Object>());
		//TODO non observational data should be captured through person attributes instead of as observations
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("telephone", telephone, null),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("telephone2", telephone2, null),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getPatientAddress("address", true, true, true, true),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getAccompRelationship("accompagnateur"),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("contactTel", contactTelephone, null),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.getMostRecent("guardianTel", guardianTelephone, null),
				new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.patientAttribute("Peer Educator's Name", "peerEducator"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.patientAttribute("Peer Educator's Phone Number", "peerEducatorPhone"), new HashMap<String, Object>());
		dataSetDefinition.addColumn(RowPerPatientColumns.patientAttribute("Phone Number", "privatePhone"), new HashMap<String, Object>());
		
		CodedObsCohortDefinition hivPositive = Cohorts.getHIVPositivePatients();
		SqlCohortDefinition adultPatientsCohort = Cohorts.getAdultPatients();
		InverseCohortDefinition notInHIVProgram = new InverseCohortDefinition(Cohorts.createInProgram("inHIVProgram", hivProgram));
		SqlCohortDefinition includeVCTClients = Cohorts.getVCTInclusiveCohortDefinition();

		dataSetDefinition.addFilter(adultPatientsCohort, ParameterizableUtil.createParameterMappings("endDate=${endDate}"));
		dataSetDefinition.addFilter(hivPositive, null);
		dataSetDefinition.addFilter(notInHIVProgram, null);
		//dataSetDefinition.addFilter(includeVCTClients, null);

		reportDefinition.addDataSetDefinition("HIVPositivePatientsDelayInLinkageToCare", dataSetDefinition, mappings);
	}

	private void setupProperties() {
		hivProgram = gp.getProgram(GlobalPropertiesManagement.ADULT_HIV_PROGRAM);
		scheduledVisit = gp.getConcept(GlobalPropertyConstants.RETURN_VISIT_CONCEPTID);
		encounterTypes = gp.getEncounterTypeList(GlobalPropertyConstants.ADULT_ENCOUNTER_TYPE_IDS);
		cd4Count = gp.getConcept(GlobalPropertyConstants.CD4_COUNT_CONCEPTID);
		viralLoad = gp.getConcept(GlobalPropertyConstants.VIRAL_LOAD_CONCEPTID);
		adultFollowUpEncounterType = gp.getEncounterType(GlobalPropertyConstants.ADULT_FOLLOWUP_ENCOUNTER_TYPEID);
		hivStatus = gp.getConcept(GlobalPropertyConstants.HIV_STATUS_CONCEPTID);
		telephone = gp.getConcept(GlobalPropertiesManagement.TELEPHONE_NUMBER_CONCEPT);
		telephone2 = gp.getConcept(GlobalPropertiesManagement.SECONDARY_TELEPHONE_NUMBER_CONCEPT);
		contactTelephone = gp.getConcept(GlobalPropertyConstants.CONTACT_TEL_CONCEPTID);
		guardianTelephone = gp.getConcept(GlobalPropertyConstants.GUARDIAN_TEL_CONCEPTID);
	}

}
