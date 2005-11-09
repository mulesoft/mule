package org.mule.tools.config.graph.processor;

import java.util.Iterator;

import org.jdom.Attribute;
import org.jdom.Element;
import org.mule.tools.config.graph.config.GraphConfig;

public class TagProcessor {

	protected final GraphConfig config;

	public TagProcessor(GraphConfig config) {
		this.config = config;
	}

	protected void appendProperties(Element element, StringBuffer caption) {
		Element properties = element.getChild("properties");
		if (properties != null) {
			for (Iterator iterator = properties.getChildren("property")
					.iterator(); iterator.hasNext();) {
				Element property = (Element) iterator.next();
				caption.append(property.getAttributeValue("name") + " :"
						+ property.getAttributeValue("value") + "\n");
			}
		}
		for (Iterator iterator = element.getAttributes().iterator(); iterator
				.hasNext();) {
			Attribute a = (Attribute) iterator.next();
			if (!ignoreAttribute(a.getName())) {
				caption.append(a.getName() + " :" + a.getValue() + "\n");
			}
		}
	}

	protected boolean ignoreAttribute(String name) {
		if (name == null || "".equals(name))
			return true;
		for (Iterator iterator = config.getIgnoredAttributes().iterator(); iterator
				.hasNext();) {
			String s = (String) iterator.next();
			if (name.equals(s)) {
				return true;
			}

		}
		return false;
	}
	protected void appendDescription(Element e, StringBuffer caption) {
		Element description = e.getChild("description");
		if (description != null) {
			caption.append("\n-------------------\n").append(
					description.getText()).append("\n");
		}
	}
}
