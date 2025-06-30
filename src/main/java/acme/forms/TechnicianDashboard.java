
package acme.forms;

import java.util.Collection;

import acme.client.components.basis.AbstractForm;
import acme.entities.aircraft.Aircraft;
import acme.entities.maintenanceRecords.MaintenanceRecord;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TechnicianDashboard extends AbstractForm {

	private static final long	serialVersionUID	= 1L;

	Integer						numberMaintenanceRecordPending;
	Integer						numberMaintenanceRecordInProgress;
	Integer						numberMaintenanceRecordCompleted;

	MaintenanceRecord			recordWithNearestInspectionDueDate;
	Collection<Aircraft>		top5AircraftsWithMostTasks;

	Double						averageEstimatedCostLastYear;
	Double						minimumEstimatedCostLastYear;
	Double						maximumEstimatedCostLastYear;
	Double						standardDeviationEstimatedCostLastYear;

	Double						averageEstimatedDurationTask;
	Integer						minimumEstimatedDurationTask;
	Integer						maximumEstimatedDurationTask;
	Double						standardDeviationEstimatedDurationTask;

}
