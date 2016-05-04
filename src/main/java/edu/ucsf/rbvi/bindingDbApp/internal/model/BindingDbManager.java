package edu.ucsf.rbvi.bindingDbApp.internal.model;

import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskMonitor;

import edu.ucsf.rbvi.bindingDbApp.internal.utils.CyUtils;
import edu.ucsf.rbvi.bindingDbApp.internal.utils.HttpUtils;

/**
 * BindingDbManager
 * 
 */
public class BindingDbManager implements SetCurrentNetworkListener {
	private static Font awesomeFont = null;
	final CyApplicationManager appManager;
	final CyEventHelper eventHelper;
	final OpenBrowser openBrowser;
	final CyServiceRegistrar serviceRegistrar;
	private boolean ignoringSelection = false;

	public static final String SMILES = "SMILES";
	public static final String Affinity = "BindingDb:Affinity";
	public static final String AffinityStr = "BindingDb:AffinityStr";
	public static final String AffinityType = "BindingDb:AffinityType";
	public static final String MonimerId = "BindingDb:MonimerId";
	public static final String HitCount = "BindingDb:Hits";
	static final String loadURI = 
					"http://bindingdb.org/axis2/services/BDBService/getLigandsByUniprot?uniprot=";

	public static Font getAwesomeFont() {
		if (awesomeFont == null) {
			try {
			awesomeFont = Font.createFont(Font.TRUETYPE_FONT,
			                              BindingDbManager.class.getResourceAsStream("/fonts/fontawesome-webfont.ttf"));
			} catch (final Exception e) {
				throw new RuntimeException("Error loading custom fonts.", e);
			}
		}
		return awesomeFont;
	}

	public BindingDbManager(CyApplicationManager appManager, 
	                        OpenBrowser openBrowser, CyServiceRegistrar serviceRegistrar) {
		this.appManager = appManager;
		this.openBrowser = openBrowser;
		this.serviceRegistrar = serviceRegistrar;
		this.eventHelper = getService(CyEventHelper.class);
	}

	public CyNetwork getCurrentNetwork() {
		return appManager.getCurrentNetwork();
	}

	public CyNetworkView getCurrentNetworkView() {
		return appManager.getCurrentNetworkView();
	}

	public String getCurrentNetworkName() {
		CyNetwork network = getCurrentNetwork();
		return network.getRow(network).get(CyNetwork.NAME, String.class);
	}

	public <S> S getService(Class<S> serviceClass) {
		return serviceRegistrar.getService(serviceClass);
	}

	public <S> S getService(Class<S> serviceClass, String filter) {
		return serviceRegistrar.getService(serviceClass, filter);
	}

	public void registerService(Object service, Class<?> serviceClass, Properties props) {
		serviceRegistrar.registerService(service, serviceClass, props);
	}

	public void unregisterService(Object service, Class<?> serviceClass) {
		serviceRegistrar.unregisterService(service, serviceClass);
	}

	public void openURL(String url) {
		if (url != null || url.length() > 0)
			openBrowser.openURL(url);
	}

	public void loadAnnotations(TaskMonitor monitor, CyNetwork network, 
	                            String idColumn, double affinityCutoff, List<CyNode> nodes) {
		if (nodes == null) nodes = network.getNodeList();

		monitor.showMessage(TaskMonitor.Level.INFO, "Loading annotations");

		// Create all of our columns (if we need to)
		CyUtils.createColumn(network.getDefaultNodeTable(), 
		                     SMILES, List.class, String.class);
		CyUtils.createColumn(network.getDefaultNodeTable(), 
		                     Affinity, List.class, Double.class);
		CyUtils.createColumn(network.getDefaultNodeTable(), 
		                     AffinityStr, List.class, String.class);
		CyUtils.createColumn(network.getDefaultNodeTable(), 
		                     AffinityType, List.class, String.class);
		CyUtils.createColumn(network.getDefaultNodeTable(), 
		                     MonimerId, List.class, String.class);
		CyUtils.createColumn(network.getDefaultNodeTable(), 
		                     HitCount, Integer.class, null);

		for (CyNode node: nodes) {
			String id = network.getRow(node).get(idColumn, String.class);
			getAnnotation(monitor, network, node, id, affinityCutoff);
		}
	}

	public void handleEvent(SetCurrentNetworkEvent scne) {
	}

	void getAnnotation(TaskMonitor monitor, CyNetwork network, CyNode node, String id, double cutoff) {
		String nodeName = CyUtils.getName(network, node);
		monitor.showMessage(TaskMonitor.Level.INFO, "Loading annotations for "+nodeName+"("+id+")");

		// Unfortunately, BindingDB uses XML...
		DocumentBuilder builder = null;
		Document annotations = null;
		int hitCount = 0;
		
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
		} catch (Exception e) {
			monitor.showMessage(TaskMonitor.Level.ERROR, "Unable to create a new document: "+e.getMessage());
			return;
		}

		try {
			annotations = HttpUtils.getXML(loadURI+id+";"+cutoff, builder);
		} catch (Exception e) {
			e.printStackTrace();
			monitor.showMessage(TaskMonitor.Level.ERROR, 
			                    "Unable to get annotations for "+id+": "+e.getMessage());
			return;
		}

		if (annotations == null) {
			monitor.showMessage(TaskMonitor.Level.ERROR, 
			                    "No annotations for "+id);
			return;
		}

		List<String> smilesList = new ArrayList<>();
		List<String> monomerIdList = new ArrayList<>();
		List<String> affinityStrList = new ArrayList<>();
		List<Double> affinityList = new ArrayList<>();
		List<String> typeList = new ArrayList<>();

		// OK, now we have all of the annotations.  Build the lists
		NodeList affinities = annotations.getElementsByTagName("bdb:affinities");

		// Iterate over all of the affinities
		for (int index = 0; index < affinities.getLength(); index++) {
			Node affinity = affinities.item(index);
			if (affinity.getNodeType() != Node.ELEMENT_NODE)
				continue;

			NodeList children = affinity.getChildNodes();
			// Iterate over all of the children to get our data
			for (int elementIndex = 0; elementIndex < children.getLength(); elementIndex++) {
				Node element = children.item(elementIndex);
				if (element.getNodeType() == Node.ELEMENT_NODE) {
					String data = getContent(element);
					if (element.getNodeName().equals("bdb:monomerid")) {
						if (monomerIdList.contains(data)) 
							continue;

						// logger.debug("Found id "+data);
						monomerIdList.add(data);
						hitCount++;
					} else if (element.getNodeName().equals("bdb:smiles")) {
						// logger.debug("Found smiles "+data);
						if (data.indexOf('|') >= 0 ) {
							String[] d = data.split("\\|");  // Get rid of extra annotation
							smilesList.add(d[0].trim());
						} else {
							smilesList.add(data.trim());
						}
					} else if (element.getNodeName().equals("bdb:affinity_type")) {
						// logger.debug("Found type "+data);
						typeList.add(data);
					} else if (element.getNodeName().equals("bdb:affinity")) {
						affinityStrList.add(data);
						// logger.debug("Found affinity "+data);
						// Special case
						if (data.indexOf('<') >= 0 ) {
							int offset = data.indexOf('<');
							double v = Double.parseDouble(data.substring(offset+1));
							affinityList.add(new Double(v/1.01));
						} else if (data.indexOf('>') >= 0 ) {
							int offset = data.indexOf('>');
							double v = Double.parseDouble(data.substring(offset+1));
							affinityList.add(new Double(v/.99));
						} else {
							affinityList.add(new Double(data));
						}
					}
				}
			}
		}
		// Now, set the values for this node
		network.getRow(node).set(MonimerId, monomerIdList);
		network.getRow(node).set(SMILES, smilesList);
		network.getRow(node).set(Affinity, affinityList);
		network.getRow(node).set(AffinityStr, affinityStrList);
		network.getRow(node).set(AffinityType, typeList);
		network.getRow(node).set(HitCount, hitCount);

		monitor.showMessage(TaskMonitor.Level.INFO, "Node: "+nodeName
		                    +" has "+hitCount+" binders");
	}

	String getContent(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE && node.hasChildNodes()) {
			Node child = node.getFirstChild();
			if (child.getNodeType() == Node.TEXT_NODE)
				return child.getNodeValue();
		}
 		return null;
	}

}
