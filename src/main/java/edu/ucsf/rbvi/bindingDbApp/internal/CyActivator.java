package edu.ucsf.rbvi.bindingDbApp.internal;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.work.TaskFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import org.cytoscape.application.CyUserLog;
import org.apache.log4j.Logger;

import edu.ucsf.rbvi.bindingDbApp.internal.model.BindingDbManager;
import edu.ucsf.rbvi.bindingDbApp.internal.tasks.LoadAnnotationsTaskFactory;

public class CyActivator extends AbstractCyActivator {
	private static Logger logger = Logger.getLogger(CyUserLog.NAME);

	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {

		// See if we have a graphics console or not
		boolean haveGUI = true;
		CySwingApplication cySwingApplication = null;
		ServiceReference ref = bc.getServiceReference(CySwingApplication.class.getName());

		if (ref == null) {
			haveGUI = false;
			// Issue error and return
		} else {
			cySwingApplication = getService(bc, CySwingApplication.class);
		}

		// Get some services we'll need
		CyApplicationManager appManager = getService(bc, CyApplicationManager.class);
		OpenBrowser openBrowser = getService(bc, OpenBrowser.class);
		CyServiceRegistrar serviceRegistrar = getService(bc, CyServiceRegistrar.class);

		// Create our manager object
		BindingDbManager manager = new BindingDbManager(appManager, openBrowser, serviceRegistrar);

		// Find binding partners
		LoadAnnotationsTaskFactory loadAnnotations = new LoadAnnotationsTaskFactory(manager);
		Properties settingsProps = new Properties();
		settingsProps.setProperty(PREFERRED_MENU, "Apps.bindingDb");
		settingsProps.setProperty(TITLE, "Load Known Binding Compounds for Network");
		settingsProps.setProperty(IN_MENU_BAR, "true");
		settingsProps.setProperty(MENU_GRAVITY, "1.0");
		registerService(bc, loadAnnotations, NetworkTaskFactory.class, settingsProps);
		registerService(bc, loadAnnotations, NodeViewTaskFactory.class, settingsProps);

	}
}
