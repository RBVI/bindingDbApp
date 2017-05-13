package edu.ucsf.rbvi.bindingDbApp.internal.model;

import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

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
	final Map<String, CyNode> idMap = new HashMap<>();

	public static final String SMILES = "BDB.SMILES";
	public static final String Affinity = "BDB.Affinity";
	public static final String AffinityStr = "BDB.AffinityStr";
	public static final String AffinityType = "BDB.AffinityType";
	public static final String MonimerId = "BDB.MonimerId";
	public static final String HitCount = "BDB.Hits";
	public static final String PMID = "BDB.PMID";
	public static final String DOI = "BDB.DOI";
	static final String loadURI = 
					"http://bindingdb.org/axis2/services/BDBService/getLigandsByUniprots";
	static final String JSON = "&response=application/json";

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
		                     PMID, List.class, String.class);
		CyUtils.createColumn(network.getDefaultNodeTable(), 
		                     DOI, List.class, String.class);
		CyUtils.createColumn(network.getDefaultNodeTable(), 
		                     HitCount, Integer.class, null);

		StringBuilder ids = new StringBuilder();

		for (CyNode node: nodes) {
			String id = network.getRow(node).get(idColumn, String.class);
			idMap.put(id, node);
			ids.append(id+",");
			CyUtils.clearColumn(network, node, SMILES, List.class, String.class);
			CyUtils.clearColumn(network, node, Affinity, List.class, Double.class);
			CyUtils.clearColumn(network, node, AffinityStr, List.class, String.class);
			CyUtils.clearColumn(network, node, AffinityType, List.class, String.class);
			CyUtils.clearColumn(network, node, MonimerId, List.class, String.class);
			CyUtils.clearColumn(network, node, PMID, List.class, String.class);
			CyUtils.clearColumn(network, node, DOI, List.class, String.class);
			CyUtils.clearColumn(network, node, HitCount, Integer.class, null);
		}
		getAnnotations(monitor, network, ids.substring(0,ids.length()-1), affinityCutoff);
	}

	public void handleEvent(SetCurrentNetworkEvent scne) {
	}

	void getAnnotations(TaskMonitor monitor, CyNetwork network, String ids, double cutoff) {
		monitor.showMessage(TaskMonitor.Level.INFO, "Loading annotations from BindingDb");

		// Initialize some counters
		int totalBinders = 0;
		Map<CyNode, Integer> nodeBindMap = new HashMap<>();
		int maxBinders = 0;
		CyNode maxEntries = null;

		Map<String, String> argMap = new HashMap<>();
		argMap.put("uniprot", ids);
		argMap.put("cutoff", String.valueOf((int)(cutoff)));
		argMap.put("code", "0");
		argMap.put("response", "application/json");

		Object annotations = null;
		try {
			annotations = HttpUtils.getJSON(loadURI, argMap);
		} catch (Exception e) {
			monitor.showMessage(TaskMonitor.Level.ERROR, "Unable to retrieve annotations: "+e.getMessage());
			return;
		}

		if (annotations instanceof JSONObject) {
			JSONObject annotationsMap = (JSONObject)annotations;
			if (!annotationsMap.containsKey("getLigandsByUniprotsResponse")) {
				monitor.showMessage(TaskMonitor.Level.WARN, "Query returned no responses");
				return;
			}
			JSONObject affMap;
		 	try {
				affMap= (JSONObject)annotationsMap.get("getLigandsByUniprotsResponse");
			} catch (ClassCastException cce) {
				monitor.showMessage(TaskMonitor.Level.WARN, "Query returned no responses");
				return;
			}
			if (!affMap.containsKey("affinities")) {
				monitor.showMessage(TaskMonitor.Level.WARN, "Query returned no responses");
				return;
			}

			JSONArray affinities = (JSONArray)affMap.get("affinities");
			for (Object affinity: affinities) {
				if (affinity instanceof JSONObject) {
					totalBinders++;
					// Get the data
					JSONObject affinityMap = (JSONObject) affinity;
					String query = (String)affinityMap.get("query");
					String monomerid = (String)affinityMap.get("monomerid");
					String smiles = (String)affinityMap.get("smile");
					String affType = (String)affinityMap.get("affinity_type");
					String aff = (String)affinityMap.get("affinity");
					String pmid = (String)affinityMap.get("pmid");
					String doi = (String)affinityMap.get("doi");
					// System.out.println("Got result for "+query+": "+smiles);

					// Save the data
					if (idMap.containsKey(query)) {
						CyNode node = idMap.get(query);
						// System.out.println("query "+query+" corresponds to node "+node);
						if (!nodeBindMap.containsKey(node))
							nodeBindMap.put(node, 0);

						int count = nodeBindMap.get(node)+1;
						if (count > maxBinders) {
							maxBinders = count;
							maxEntries = node;
						}
						nodeBindMap.put(node, count);

						CyUtils.appendToList(network, node, SMILES, String.class, smiles);
						CyUtils.appendToList(network, node, AffinityStr, String.class, aff);
						CyUtils.appendToList(network, node, Affinity, Double.class, convertAffinity(aff));
						CyUtils.appendToList(network, node, AffinityType, String.class, affType);
						CyUtils.appendToList(network, node, MonimerId, String.class, monomerid);
						CyUtils.appendToList(network, node, PMID, String.class, pmid);
						CyUtils.appendToList(network, node, DOI, String.class, doi);
						CyUtils.addInteger(network, node, HitCount, 1);
					}

				}
			}
		}

		monitor.showMessage(TaskMonitor.Level.INFO, 
		                    "Loaded "+totalBinders+" annotations for "+nodeBindMap.size()+" nodes. "+
												"Node "+CyUtils.getName(network, maxEntries)+
												" has the most entries: "+maxBinders);

	}

	String getContent(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE && node.hasChildNodes()) {
			Node child = node.getFirstChild();
			if (child.getNodeType() == Node.TEXT_NODE)
				return child.getNodeValue();
		}
 		return null;
	}

	Double convertAffinity(String data) {
		// Special case
		if (data.indexOf('<') >= 0 ) {
			int offset = data.indexOf('<');
			double v = Double.parseDouble(data.substring(offset+1));
			return new Double(v/1.01);
		} else if (data.indexOf('>') >= 0 ) {
			int offset = data.indexOf('>');
			double v = Double.parseDouble(data.substring(offset+1));
			return new Double(v/.99);
		} 
		return new Double(data);
	}

}
