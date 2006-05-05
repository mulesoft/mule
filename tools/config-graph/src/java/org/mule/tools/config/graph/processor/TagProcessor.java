package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphEdge;
import com.oy.shared.lm.graph.GraphNode;
import org.apache.commons.lang.StringUtils;
import org.jdom.Attribute;
import org.jdom.Element;
import org.mule.tools.config.graph.config.GraphEnvironment;
import org.mule.tools.config.graph.util.MuleTag;

import java.util.Iterator;
import java.util.List;

public abstract class TagProcessor {

    protected static GraphEnvironment environment;


    public TagProcessor(GraphEnvironment env) {
        environment = env;
    }

    public abstract void process(Graph graph, Element currentElement, GraphNode parent);

    public static void appendProperties(Element element, StringBuffer caption) {
        Element properties = element.getChild(MuleTag.ELEMENT_PROPERTIES);
        if (properties != null) {
            for (Iterator iterator = properties.getChildren(MuleTag.ELEMENT_PROPERTY)
                    .iterator(); iterator.hasNext();) {
                Element property = (Element) iterator.next();
                caption.append(property.getAttributeValue(MuleTag.ATTRIBUTE_NAME) + " :"
                        + lookupPropertyTemplate(property.getAttributeValue(MuleTag.ATTRIBUTE_VALUE)) + "\n");
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

    protected static boolean ignoreAttribute(String name) {
        if (name == null || "".equals(name))
            return true;
        for (Iterator iterator = environment.getConfig().getIgnoredAttributes().iterator(); iterator
                .hasNext();) {
            String s = (String) iterator.next();
            if (name.equals(s)) {
                return true;
            }

        }
        return false;
    }
    public static void appendDescription(Element e, StringBuffer caption) {
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
        if(value!=null) caption.append(name + " = " + (StringUtils.EMPTY.equals(value) ? "\"\"" : lookupPropertyTemplate(value)) + "\n");
    }



    public static void addEdge(Graph graph, GraphNode src, GraphNode dest, String caption, boolean twoway) {
        GraphEdge ge = graph.addEdge(src, dest);
        if(twoway) {
            ge.getInfo().setArrowTailNormal();
        }

        if(caption!=null) {
            if("in".equalsIgnoreCase(caption) && twoway) {
                caption += " / out";
            }else if("out".equalsIgnoreCase(caption) && twoway) {
                caption += " / in";
            }
            ge.getInfo().setCaption(caption);
        }
    }

    public static void addRelation(Graph graph, GraphNode src, GraphNode dest, String caption) {
        GraphEdge ge = graph.addEdge(src, dest);
        ge.getInfo().setArrowHeadNone();
        if(caption!=null) ge.getInfo().setCaption(caption);
    }

    public boolean isTwoWay(Element e) {
        if(e==null) return environment.isDefaultTwoWay();
       return ("true".equalsIgnoreCase(e.getAttributeValue(MuleTag.ATTRIBUTE_SYNCHRONOUS)) || environment.isDefaultTwoWay());
    }

    protected static String lookupPropertyTemplate(String template) {
        if(template==null) return null;
        String value = environment.getProperties().getProperty(template, null);
        if(value==null && template.startsWith("${")) {
            value = environment.getProperties().getProperty(template.substring(2, template.length()-1), template);
        } else {
            value = template;
        }
        return value;
    }
}
