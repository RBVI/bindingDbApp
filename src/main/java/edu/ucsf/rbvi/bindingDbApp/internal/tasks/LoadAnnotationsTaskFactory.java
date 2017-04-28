package edu.ucsf.rbvi.bindingDbApp.internal.tasks;

import java.util.ArrayList;
import java.util.List;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.TaskIterator;

import edu.ucsf.rbvi.bindingDbApp.internal.model.BindingDbManager;

public class LoadAnnotationsTaskFactory extends AbstractNetworkTaskFactory implements NodeViewTaskFactory {
	final BindingDbManager bindingDbManager;

	public LoadAnnotationsTaskFactory(BindingDbManager manager) {
		this.bindingDbManager = manager;
	}

	@Override
	public TaskIterator createTaskIterator(CyNetwork net) {
		List<CyNode> nodes = net.getNodeList();
		return new TaskIterator(new LoadAnnotationsTask(net, nodes, bindingDbManager));
	}

	@Override
	public TaskIterator createTaskIterator(View<CyNode> nodeView, CyNetworkView netView) {
		List<CyNode> nodes = new ArrayList<CyNode>();
		nodes.addAll(CyTableUtil.getNodesInState(netView.getModel(), CyNetwork.SELECTED, true));
		if (!nodes.contains(nodeView.getModel()))
			nodes.add(nodeView.getModel());
		return new TaskIterator(new LoadAnnotationsTask(netView.getModel(), nodes, bindingDbManager));
	}

	@Override
	public boolean isReady(View<CyNode> nodeView, CyNetworkView netView) {
		if (nodeView == null) return false;
		return true;
	}
}
