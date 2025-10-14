
package acme.features.technician.maintenanceRecords;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import acme.client.components.models.Dataset;
import acme.client.components.views.SelectChoices;
import acme.client.services.AbstractGuiService;
import acme.client.services.GuiService;
import acme.entities.aircraft.Aircraft;
import acme.entities.maintenanceRecords.MaintenanceRecord;
import acme.entities.maintenanceRecords.MaintenanceRecordStatus;
import acme.realms.technician.Technician;

@GuiService
public class TechnicianMaintenanceRecordShowService extends AbstractGuiService<Technician, MaintenanceRecord> {

	@Autowired
	private TechnicianMaintenanceRecordRepository repository;


	@Override
	public void authorise() {
		MaintenanceRecord mrecord;
		Technician technician;
		int id;

		technician = (Technician) super.getRequest().getPrincipal().getActiveRealm();

		id = super.getRequest().getData("id", int.class);
		mrecord = this.repository.findMaintenanceRecordById(id);

		if (mrecord == null)
			super.getResponse().setAuthorised(false);
		else
			super.getResponse().setAuthorised(mrecord.getTechnician().equals(technician));
	}

	@Override
	public void load() {
		MaintenanceRecord maintenanceRecord;

		int id;

		id = super.getRequest().getData("id", int.class);
		maintenanceRecord = this.repository.findMaintenanceRecordById(id);

		super.getBuffer().addData(maintenanceRecord);
	}

	@Override
	public void unbind(final MaintenanceRecord maintenanceRecord) {
		Dataset dataset;
		SelectChoices choicesStatuses;
		SelectChoices choicesAircraft;

		choicesStatuses = SelectChoices.from(MaintenanceRecordStatus.class, maintenanceRecord.getStatus());
		Collection<Aircraft> aircrafts = this.repository.findAllAircraft();
		choicesAircraft = SelectChoices.from(aircrafts, "registrationNumber", maintenanceRecord.getRelatedAircraft());

		dataset = super.unbindObject(maintenanceRecord, "maintenanceMoment", "status", "nextInspection", "estimatedCost", "notes", "draftMode");
		dataset.put("relatedAircraft", choicesAircraft.getSelected().getKey());
		dataset.put("aircrafts", choicesAircraft);
		dataset.put("statuses", choicesStatuses);

		super.getResponse().addData(dataset);
	}

}
