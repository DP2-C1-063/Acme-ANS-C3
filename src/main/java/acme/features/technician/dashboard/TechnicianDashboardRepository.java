
package acme.features.technician.dashboard;

import java.util.Collection;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import acme.client.repositories.AbstractRepository;
import acme.entities.aircraft.Aircraft;
import acme.entities.maintenanceRecords.MaintenanceRecord;
import acme.entities.maintenanceRecords.MaintenanceRecordStatus;

@Repository
public interface TechnicianDashboardRepository extends AbstractRepository {

	@Query("select count(mr) from MaintenanceRecord mr where mr.technician.id = :technicianId and mr.status = :status and mr.draftMode = false ")
	Integer findNumberMaintenanceRecordByStatus(int technicianId, MaintenanceRecordStatus status);

	@Query("select mr from MaintenanceRecord mr where mr.technician.id = :technicianId and mr.draftMode = false and exists (select i from TaskInvolvesRecord i where i.maintenanceRecord = mr) order by mr.nextInspection ASC ")
	Collection<MaintenanceRecord> findRecordWithNearestInspectionDueDate(int technicianId);

	@Query(" select mr.relatedAircraft from MaintenanceRecord mr join TaskInvolvesRecord i on i.maintenanceRecord = mr where mr.draftMode = false and mr.technician.id = :technicianId group by mr.relatedAircraft order by count(i) desc")
	Collection<Aircraft> findTop5AircraftsWithMostTasks(int technicianId);

	@Query("select avg(mr.estimatedCost.amount) from MaintenanceRecord mr where mr.technician.id = :technicianId and mr.draftMode = false and YEAR(mr.maintenanceMoment) = :currentYear")
	Double findAverageEstimatedCostLastYear(int technicianId, int currentYear);

	@Query("select min(mr.estimatedCost.amount) from MaintenanceRecord mr where mr.technician.id = :technicianId and mr.draftMode = false  and YEAR(mr.maintenanceMoment) = :currentYear")
	Double findMinimumEstimatedCostLastYear(int technicianId, int currentYear);

	@Query("select max(mr.estimatedCost.amount) from MaintenanceRecord mr where mr.technician.id = :technicianId and mr.draftMode = false  and YEAR(mr.maintenanceMoment) = :currentYear")
	Double findMaximumEstimatedCostLastYear(int technicianId, int currentYear);

	@Query("select stddev(mr.estimatedCost.amount) from MaintenanceRecord mr where mr.technician.id = :technicianId and mr.draftMode = false and YEAR(mr.maintenanceMoment) = :currentYear")
	Double findSTDDEVEstimatedCostLastYear(int technicianId, int currentYear);

	@Query("select avg(t.estimatedDuration) from Task t where t.technician.id=:technicianId and t.draftMode = false")
	Double findAverageEstimatedDurationTask(int technicianId);

	@Query("select min(t.estimatedDuration) from Task t where t.technician.id=:technicianId and t.draftMode = false")
	Integer findMinimumEstimatedDurationTask(int technicianId);

	@Query("select max(t.estimatedDuration) from Task t where t.technician.id=:technicianId and t.draftMode = false")
	Integer findMaximumEstimatedDurationTask(int technicianId);

	@Query("select stddev(t.estimatedDuration) from Task t where t.technician.id=:technicianId and t.draftMode = false")
	Double findSTDDEVEstimatedDurationTask(int technicianId);

}
