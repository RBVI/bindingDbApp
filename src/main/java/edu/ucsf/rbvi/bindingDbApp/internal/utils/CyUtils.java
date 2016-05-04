package edu.ucsf.rbvi.bindingDbApp.internal.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;

/**
 * Utilities for various cytoscape functions
 * 
 *
 */
public class CyUtils {

	public static void createColumn(CyTable table, String columnName, Class<?> type, Class<?> elementType) {
		CyColumn column = table.getColumn(columnName);
		if (column != null) {
			if (!column.getType().equals(type))
				throw new RuntimeException("Column "+columnName+" already exists, but has a different type");
			if (column.getType().equals(List.class) && !column.getListElementType().equals(elementType))
				throw new RuntimeException("List column "+columnName+" already exists, but has a different element type");
			return;
		}
		if (type.equals(List.class))
			table.createListColumn(columnName, elementType, false);
		else
			table.createColumn(columnName, type, false);
		return;
	}

	public static boolean checkColumn(CyTable table, String columnName, Class<?> type, Class<?> elementType) {
		CyColumn column = table.getColumn(columnName);
		if (column == null || column.getType() != type)
			return false;
		if (type.equals(List.class) && column.getListElementType() != elementType)
			return false;
		return true;
	}

	public static String getName(CyNetwork network, CyIdentifiable id) {
		return network.getRow(id).get(CyNetwork.NAME, String.class);
	}

	public static CyIdentifiable getIdentifiable(CyNetwork network, Long suid) {
		if (network.getNode(suid) != null)
			return (CyIdentifiable)network.getNode(suid);
		else if (network.getEdge(suid) != null)
			return (CyIdentifiable)network.getEdge(suid);
		else
			return (CyIdentifiable)network;
	}
}
