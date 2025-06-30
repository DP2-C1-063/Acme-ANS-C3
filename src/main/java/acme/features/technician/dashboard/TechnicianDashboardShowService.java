
package acme.features.technician.dashboard;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.aircraft.Aircraft;
import acme.entities.maintenanceRecords.MaintenanceRecord;
import acme.entities.maintenanceRecords.MaintenanceRecordStatus;
import acme.forms.TechnicianDashboard;
import acme.realms.technician.Technician;

@GuiService
public class TechnicianDashboardShowService extends AbstractGuiService<Technician, TechnicianDashboard> {

	@Autowired
	private TechnicianDashboardRepository repository;


	@Override
	public void authorise() {
		boolean status = super.getRequest().getPrincipal().hasRealmOfType(Technician.class);
		super.getResponse().setAuthorised(status);
	}

	@Override
	public void load() {
		TechnicianDashboard dashboard;
		int technicianId = this.getRequest().getPrincipal().getActiveRealm().getId();

		Integer numberMaintenanceRecordPending;
		Integer numberMaintenanceRecordInProgress;
		Integer numberMaintenanceRecordCompleted;
		MaintenanceRecord recordWithNearestInspection;
		List<Aircraft> top5AircraftsWithMostTasks;
		Double averageEstimatedCostLastYear;
		Double minimumEstimatedCostLastYear;
		Double maximumEstimatedCostLastYear;
		Double standardDeviationEstimatedCostLastYear;

		Double averageEstimatedDurationTask;
		Integer minimumEstimatedDurationTask;
		Integer maximumEstimatedDurationTask;
		Double standardDeviationEstimatedDurationTask;
		int currentYear = LocalDate.now().getYear() - 1;

		numberMaintenanceRecordPending = this.repository.findNumberMaintenanceRecordByStatus(technicianId, MaintenanceRecordStatus.PENDING);
		numberMaintenanceRecordInProgress = this.repository.findNumberMaintenanceRecordByStatus(technicianId, MaintenanceRecordStatus.IN_PROGRESS);
		numberMaintenanceRecordCompleted = this.repository.findNumberMaintenanceRecordByStatus(technicianId, MaintenanceRecordStatus.COMPLETED);

		Collection<MaintenanceRecord> records = this.repository.findRecordWithNearestInspectionDueDate(technicianId);
		recordWithNearestInspection = records.isEmpty() ? null : records.iterator().next();
		top5AircraftsWithMostTasks = this.repository.findTop5AircraftsWithMostTasks(technicianId).stream().limit(5).collect(Collectors.toList());

		averageEstimatedCostLastYear = this.repository.findAverageEstimatedCostLastYear(technicianId, currentYear);
		minimumEstimatedCostLastYear = this.repository.findMinimumEstimatedCostLastYear(technicianId, currentYear);
		maximumEstimatedCostLastYear = this.repository.findMaximumEstimatedCostLastYear(technicianId, currentYear);
		standardDeviationEstimatedCostLastYear = this.repository.findSTDDEVEstimatedCostLastYear(technicianId, currentYear);

		averageEstimatedDurationTask = this.repository.findAverageEstimatedDurationTask(technicianId);
		minimumEstimatedDurationTask = this.repository.findMinimumEstimatedDurationTask(technicianId);
		maximumEstimatedDurationTask = this.repository.findMaximumEstimatedDurationTask(technicianId);
		standardDeviationEstimatedDurationTask = this.repository.findSTDDEVEstimatedDurationTask(technicianId);

		dashboard = new TechnicianDashboard();
		dashboard.setNumberMaintenanceRecordPending(numberMaintenanceRecordPending);
		dashboard.setNumberMaintenanceRecordInProgress(numberMaintenanceRecordInProgress);
		dashboard.setNumberMaintenanceRecordCompleted(numberMaintenanceRecordCompleted);
		dashboard.setRecordWithNearestInspectionDueDate(recordWithNearestInspection);
		dashboard.setTop5AircraftsWithMostTasks(top5AircraftsWithMostTasks);
		dashboard.setAverageEstimatedCostLastYear(averageEstimatedCostLastYear);
		dashboard.setMinimumEstimatedCostLastYear(minimumEstimatedCostLastYear);
		dashboard.setMaximumEstimatedCostLastYear(maximumEstimatedCostLastYear);
		dashboard.setStandardDeviationEstimatedCostLastYear(standardDeviationEstimatedCostLastYear);

		dashboard.setAverageEstimatedDurationTask(averageEstimatedDurationTask);
		dashboard.setMinimumEstimatedDurationTask(minimumEstimatedDurationTask);
		dashboard.setMaximumEstimatedDurationTask(maximumEstimatedDurationTask);
		dashboard.setStandardDeviationEstimatedDurationTask(standardDeviationEstimatedDurationTask);

		super.getBuffer().addData(dashboard);
	}

	@Override
	public void unbind(final TechnicianDashboard dashboard) {
		Dataset dataset;

		dataset = super.unbindObject(dashboard, "numberMaintenanceRecordPending", "numberMaintenanceRecordInProgress", "numberMaintenanceRecordCompleted", "recordWithNearestInspectionDueDate", "top5AircraftsWithMostTasks", "averageEstimatedCostLastYear",
			"minimumEstimatedCostLastYear", "maximumEstimatedCostLastYear", "standardDeviationEstimatedCostLastYear", "averageEstimatedDurationTask", "minimumEstimatedDurationTask", "maximumEstimatedDurationTask", "standardDeviationEstimatedDurationTask");

		super.getResponse().addData(dataset);
	}
}
