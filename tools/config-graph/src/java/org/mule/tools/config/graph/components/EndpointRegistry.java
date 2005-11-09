package org.mule.tools.config.graph.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.jdom.Element;
import org.mule.tools.config.graph.config.ColorRegistry;
import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.processor.TagProcessor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

public class EndpointRegistry extends TagProcessor{

	private Map endpoints = new HashMap();
	public EndpointRegistry(GraphConfig config) {
		super(config);
	}
	
	public GraphNode[] getVirtualEndpoint(String componentName) {

		List nodesList = new ArrayList();
		String mappedUri = config.getMappings().getProperty(componentName);
		if (mappedUri != null) {
			StringTokenizer stringTokenizer = new StringTokenizer(mappedUri,
					",");
			while (stringTokenizer.hasMoreTokens()) {
				String s = stringTokenizer.nextToken();
				System.out.println("Mapping virtual endpoint '" + s
						+ "' for component '" + componentName + "'");
				GraphNode n = getEndpoint(s, componentName);
				if (n != null)
					nodesList.add(n);
			}
		}

		GraphNode[] nodes = null;
		if (nodesList.size() > 0) {
			nodes = new GraphNode[nodesList.size()];
			nodes = (GraphNode[]) nodesList.toArray(nodes);
		} else {
			nodes = new GraphNode[] {};
		}
		return nodes;
	}
	
	public GraphNode getEndpoint(String uri, String componentName) {
		GraphNode n = getEqualsMapping(uri, componentName);
		if (n == null)
			n = (GraphNode) endpoints.get(uri);
		if (n == null) {
			for (Iterator iterator = endpoints.keySet().iterator(); iterator
					.hasNext();) {
				String s = (String) iterator.next();
				if (s.startsWith(uri + "/" + componentName)) {
					n = (GraphNode) endpoints.get(s);
				}
			}
		}
		
		return n;
	}
	
	private GraphNode getEqualsMapping(String uri, String componentName) {
		String equalsMapping = config.getMappings()
				.getProperty(uri + ".equals");
		if (equalsMapping != null) {
			System.out.println("Mapping equivilent endpoint '" + equalsMapping
					+ "' to '" + uri + "'");
			return getEndpoint(equalsMapping, componentName);
		}
		return null;
	}

	public void addEndpoint(String url, GraphNode out) {
		endpoints.put(url, out);		
	}
	
	public void parseEndpoints(Graph graph, Element root) {
		Element globalEndpoints = root.getChild("global-endpoints");

		if (globalEndpoints == null) {
			System.out.println("no global-endpoints");
			return;
		}

		List namedChildren = globalEndpoints.getChildren("endpoint");

		for (Iterator iter = namedChildren.iterator(); iter.hasNext();) {
			Element endpoint = (Element) iter.next();
			GraphNode node = graph.addNode();
			node.getInfo().setFillColor(ColorRegistry.COLOR_DEFINED_ENDPOINTS);
			String name = endpoint.getAttributeValue("name");

			node.getInfo().setHeader(
					endpoint.getAttributeValue("address") + " (" + name + ")");
			StringBuffer caption = new StringBuffer();
			appendProperties(endpoint, caption);
			node.getInfo().setCaption(caption.toString());
			endpoints.put(name, node);

			// processFilter(graph, endpoint, node);
		}
	}
	
	public void parseEndpointIdentifiers(Graph graph, Element root) {
		Element endpointIdentifiers = root.getChild("endpoint-identifiers");

		if (endpointIdentifiers == null) {
			System.out.println("no endpoint-identifiers tag");
			return;
		}

		List namedChildren = endpointIdentifiers
				.getChildren("endpoint-identifier");

		for (Iterator iter = namedChildren.iterator(); iter.hasNext();) {
			Element endpoint = (Element) iter.next();
			GraphNode node = graph.addNode();
			node.getInfo().setFillColor(ColorRegistry.COLOR_DEFINED_ENDPOINTS);
			String name = endpoint.getAttributeValue("name");

			node.getInfo().setHeader(name);
			node.getInfo().setCaption(endpoint.getAttributeValue("value"));
			addEndpoint(name, node);
		}
	}
}
