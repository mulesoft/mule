package org.mule.tools.config.graph.processor;

import org.jdom.Attribute;
import org.jdom.Element;
import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.util.MuleTag;
import org.mule.util.Utility;

import java.util.Iterator;
import java.util.List;

public class TagProcessor {

	protected final GraphConfig config;

	public TagProcessor(GraphConfig config) {
		this.config = config;
	}

	protected void appendProperties(Element element, StringBuffer caption) {
		Element properties = element.getChild(MuleTag.ELEMENT_PROPERTIES);
		if (properties != null) {
			for (Iterator iterator = properties.getChildren(MuleTag.ELEMENT_PROPERTY)
					.iterator(); iterator.hasNext();) {
				Element property = (Element) iterator.next();
				caption.append(property.getAttributeValue(MuleTag.ATTRIBUTE_NAME) + " :"
						+ property.getAttributeValue(MuleTag.ATTRIBUTE_VALUE) + "\n");
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
		Element description = e.getChild(MuleTag.ELEMENT_DESCRIPTION);
		if (description != null) {
			caption.append("\n-------------------\n").append(
					description.getText()).append("\n");
		}
	}

    protected void appendProfiles(Element descriptor, StringBuffer caption) {
		List elements  = descriptor.getChildren(MuleTag.ELEMENT_THREADING_PROFILE);
        for (Iterator iterator = elements.iterator(); iterator.hasNext();) {
            Element threadingProfile = (Element) iterator.next();
            if (threadingProfile != null) {
                appendAttribute(threadingProfile, "maxBufferSize", caption);
                appendAttribute(threadingProfile, "threadTTL", caption);
                appendAttribute(threadingProfile, "maxThreadsActive", caption);
                appendAttribute(threadingProfile, "maxThreadsIdle", caption);
                appendAttribute(threadingProfile, "id", caption);
            }
        }
		Element poolingProfile = descriptor.getChild(MuleTag.ELEMENT_POOLING_PROFILE);
		if (poolingProfile != null) {
			appendAttribute(poolingProfile, "exhaustedAction", caption);
			appendAttribute(poolingProfile, "maxActive", caption);
			appendAttribute(poolingProfile, "maxIdle", caption);
			appendAttribute(poolingProfile, "maxWait", caption);
		}

        Element queueProfile = descriptor.getChild(MuleTag.ELEMENT_QUEUE_PROFILE);
		if (queueProfile != null) {
			appendAttribute(poolingProfile, "maxOutstandingMessages", caption);
			appendAttribute(poolingProfile, "persistent", caption);
		}
	}

    protected void appendAttribute(Element e, String name, StringBuffer caption) {
        if(e.getAttribute(name) == null) return;
        String value = e.getAttributeValue(name);
        if(value!=null) caption.append(name + " = " + (Utility.EMPTY_STRING.equals(value) ? "\"\"" : value) + "\n");
    }
}
