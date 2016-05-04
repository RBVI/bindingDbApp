package edu.ucsf.rbvi.bindingDbApp.internal.tasks;

import java.util.ArrayList;
import java.util.List;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.BoundedDouble;
import org.cytoscape.work.util.ListSingleSelection;

import edu.ucsf.rbvi.bindingDbApp.internal.model.BindingDbManager;

public class LoadAnnotationsTask extends AbstractTask {
	@Tunable(description="Choose column containing Uniprot identifier", gravity=10)
	public ListSingleSelection<String> idColumn; // Column to load

	@Tunable(description="Affinity cutoff", params="slider=true", gravity=20)
	public BoundedDouble affinityCutoff = new BoundedDouble(0.0, 100.0, 500.0, false, false);

	private CyNetwork network;

	final BindingDbManager bindingDbManager;

	/**
	 * Constructor for loading CDD Domain from the CDD website.
	 * @param net CyNetwork to load the domain.
	 * @param manager The CDD Domain manager
	 */
	public LoadAnnotationsTask(CyNetwork net, BindingDbManager manager) {
		super();
		this.bindingDbManager = manager;

		if (net != null)
			this.network = net;
		else
			this.network = bindingDbManager.getCurrentNetwork();

		List<String> columns = new ArrayList<>();
		for (CyColumn column: network.getDefaultNodeTable().getColumns()) {
			if (column.getType().equals(String.class)) {
				columns.add(column.getName());
			}
		}
		idColumn = new ListSingleSelection<>(columns);

	}

	@ProvidesTitle
	public String getTitle() { return "Set Column Values"; }
	
	@Override
	public void run(TaskMonitor monitor) throws Exception {
		monitor.setTitle("Load Binding Compounds");
		if (network == null)
			network = bindingDbManager.getCurrentNetwork();

		bindingDbManager.loadAnnotations(monitor, network, 
		                                 idColumn.getSelectedValue(), affinityCutoff.getValue(),
																		 null);
	}

}
