package edu.ucsf.rbvi.bindingDbApp.internal.tasks;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.bindingDbApp.internal.model.BindingDbManager;

public class LoadAnnotationsTaskFactory extends AbstractNetworkTaskFactory {
	final BindingDbManager bindingDbManager;

	public LoadAnnotationsTaskFactory(BindingDbManager manager) {
		this.bindingDbManager = manager;
	}

	public TaskIterator createTaskIterator(CyNetwork net) {
		// TODO Auto-generated method stub
		return new TaskIterator(new LoadAnnotationsTask(net, bindingDbManager));
	}

}
