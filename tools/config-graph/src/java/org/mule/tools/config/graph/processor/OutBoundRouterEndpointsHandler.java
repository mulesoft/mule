package org.mule.tools.config.graph.processor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

import org.jdom.Element;
import org.mule.tools.config.graph.config.GraphEnvironment;
import org.mule.tools.config.graph.util.MuleTag;

import java.util.Iterator;
import java.util.List;

public class OutBoundRouterEndpointsHandler extends TagProcessor{

    private String componentName;

    public OutBoundRouterEndpointsHandler( GraphEnvironment environment, String componentName) {
        super(environment);
        this.componentName = componentName;

    }

    public void process(Graph graph, Element currentElement, GraphNode parent) {
        List epList = currentElement.getChildren(MuleTag.ELEMENT_ENDPOINT);
        int x=1;
        for (Iterator iterator = epList.iterator(); iterator.hasNext(); x++) {
            Element outEndpoint = (Element) iterator.next();

            String url = outEndpoint
                    .getAttributeValue(MuleTag.ATTRIBUTE_ADDRESS);
            if (url != null) {
                GraphNode out = environment.getEndpointRegistry().getEndpoint(url, componentName);
                if (out == null) {
                    out = graph.addNode();
                    StringBuffer caption = new StringBuffer();
                    //caption.append(url).append("\n");
                    appendProperties(outEndpoint, caption);
                    appendDescription(outEndpoint, caption);
                    out.getInfo().setCaption(caption.toString());
                    environment.getEndpointRegistry().addEndpoint(url, out);
                    processOutboundFilter(graph, outEndpoint, out, parent);
                } else {
                    String caption = "out";
                    if(epList.size()>1) {
                        caption +=" (" + x + " of " + epList.size() + ")";
                    }
                    addEdge(graph, parent, out, caption, isTwoWay(outEndpoint));

                }
            }

            GraphNode[] virtual = environment.getEndpointRegistry().getVirtualEndpoint(componentName);
            if (virtual.length > 0) {
                for (int i = 0; i < virtual.length; i++) {
                     addEdge(graph, parent, virtual[i], "out (dynamic)", isTwoWay(outEndpoint));
                }
            }
        }
    }

    private void processOutboundFilter(Graph graph, Element outEndpoint,
            GraphNode out, GraphNode routerNode) {

        OutboundFilterProcessor processor = new OutboundFilterProcessor(environment, out);
        processor.process(graph, outEndpoint, routerNode);
    }

}
