package org.mule.tools.config.graph;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphFactory;
import com.oy.shared.lm.graph.GraphNode;
import com.oy.shared.lm.out.GRAPHtoDOTtoGIF;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.mule.config.MuleDtdResolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class MuleGrapher {
    // colors http://www.graphviz.org/pub/scm/graphviz2/doc/info/colors.html

    private static final String TAG_ATTRIBUTE_ADDRESS = "address";
    private static final String TAG_ENDPOINT = "endpoint";
    private static final String COLOR_DEFINED_ENDPOINTS = "lightblue";
    private static final String COLOR_ROUTER = "darkolivegreen3";
    private static final String COLOR_FILTER = "gold";
    private static final String COLOR_ENDPOINT = "white";

    private GraphConfig config = null;
    private Map endpoints = new HashMap();

    public static void main(String[] args) {

        if(args.length==0 || args[0].equals(GraphConfig.ARG_HELP)) {
            printUsage();
            System.exit(0);
        }
        MuleGrapher grapher = null;
        try {
            grapher = new MuleGrapher(new GraphConfig(args));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        }
        grapher.run();
    }

    public MuleGrapher(GraphConfig config) {
        this.config = config;
    }

        public void run() {
            try {
                config.validate();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                System.exit(0);
            }
            try {
                String filename = config.getOutputFilename();
            if(config.isCombineFiles()) {
                if(filename==null) filename = config.getFiles().get(0).toString() + ".combined";
                generateGraph(config.getFiles(), config.getOutputDirectory(), config.getCaption(), filename);
            } else {
                for (Iterator iterator = config.getFiles().iterator(); iterator.hasNext();) {
                    String s = (String) iterator.next();
                    List list = new ArrayList(1);
                    list.add(s);

                    generateGraph(list, config.getOutputDirectory(), config.getCaption(), new File(s).getName());
                }
            }

            if(!config.isKeepDotFiles()) {
                File[] dotFiles = config.getOutputDirectory().listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.endsWith(".dot");
                    }
                });
                for (int x = 0; x < dotFiles.length; x++) {
                    dotFiles[x].delete();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }



    protected Graph generateGraph(List files, File outputDir, String caption, String fileName) throws JDOMException, IOException {
        endpoints.clear();
        SAXBuilder builder = new SAXBuilder();
        builder.setValidation(true);
        builder.setEntityResolver(new MuleDtdResolver());
        Graph graph = GraphFactory.newGraph();

        builder.setIgnoringElementContentWhitespace(true);
        for (Iterator iterator = files.iterator(); iterator.hasNext();) {
            String s = (String) iterator.next();
            File myFile = new File(s);
            System.out.println("processing file " + myFile.getCanonicalPath());
            Document doc = builder.build(myFile);


            Element root = doc.getRootElement();
            if(files.size() == 1) {
                if(caption==null) {
                    caption = root.getAttribute("id").getValue();
                    if(caption!=null) {
                        caption = caption.replaceAll("_", " ");
                    } else {
                        caption = "Mule Configuration";
                    }
                }
                StringBuffer captionBuffer = new StringBuffer();
                captionBuffer.append(caption);
                appendDescription(root, captionBuffer);
                graph.getInfo().setCaption(captionBuffer.toString());
            }

            parseEndpointIdentifiers(graph, root);
            parseEndpoints(graph, root);

            Element model = root.getChild("model");
            if (model != null) {
                parseModel(graph, model);

            } else {
                parseModel(graph, root);

            }
            parseConnectors(graph, root);
        }
        if(files.size() > 1) {
            if(caption==null) caption = "(no caption set)";
            graph.getInfo().setCaption(caption);
        }

        clearHiddenNodes(graph);
        saveGraph(graph, fileName, outputDir);

        return graph;
    }

    private void clearHiddenNodes(Graph graph) {
        if(config.getMappings().size()>0) {
            GraphNode[] nodes = graph.getNodes();
            for (int i = 0; i < nodes.length; i++) {
                GraphNode node = nodes[i];
                boolean hide = Boolean.valueOf(config.getMappings().getProperty(node.getInfo().getHeader() + ".hide", "false")).booleanValue();
                if(hide) {
                    System.out.println("Hiding node '" + node.getInfo().getHeader() + "'");
                    graph.removeNode(node);
                }
            }
        }
    }

    private void parseConnectors(Graph graph, Element root) {
        List connectorsElement = root.getChildren("connector");
        for (Iterator iter = connectorsElement.iterator(); iter.hasNext();) {
            Element connector = (Element) iter.next();
            GraphNode connectorNode = graph.addNode();
            connectorNode.getInfo().setFillColor("grey91");
            String name = connector.getAttributeValue("name");
            connectorNode.getInfo().setHeader(name);

            StringBuffer caption = new StringBuffer();

            String className = connector.getAttributeValue("className");
            caption.append("className : " + className + "\n");

            appendProperties(connector, caption);
            appendDescription(connector, caption);
            connectorNode.getInfo().setCaption(caption.toString());
        }
    }

    private void appendProperties(Element element, StringBuffer caption) {
        Element properties = element.getChild("properties");
        if (properties != null) {
            for (Iterator iterator = properties.getChildren("property")
                    .iterator(); iterator.hasNext();) {
                Element property = (Element) iterator.next();
                caption.append(property.getAttributeValue("name") + " :"
                        + property.getAttributeValue("value") + "\n");
            }
        }
        for (Iterator iterator = element.getAttributes().iterator(); iterator.hasNext();) {
            Attribute a = (Attribute) iterator.next();
            if(!ignoreAttribute(a.getName())) {
                caption.append(a.getName() + " :"
                        + a.getValue() + "\n");
            }
        }
    }

    private boolean ignoreAttribute(String name) {
        if(name==null || "".equals(name)) return true;
        for (Iterator iterator = config.getIgnoredAttributes().iterator(); iterator.hasNext();) {
            String s = (String) iterator.next();
            if(name.equals(s)) {
                return true;
            }

        }
        return false;
    }

    private void appendDescription(Element e, StringBuffer caption) {
        Element description = e.getChild("description");
        if (description != null) {
            caption.append("\n-------------------\n").append(description.getText()).append("\n");
        }
    }

    private void parseModel(Graph graph, Element model) {
        if (model == null) {
            System.err.println("model is null");
            return;
        }
        List descriptors = model.getChildren("mule-descriptor");
        for (Iterator iter = descriptors.iterator(); iter.hasNext();) {
            Element descriptor = (Element) iter.next();
            String name = descriptor.getAttributeValue("name");
            GraphNode node = graph.addNode();
            node.getInfo().setHeader(name);
            node.getInfo().setFillColor("grey");

            StringBuffer caption = new StringBuffer();

            caption.append(descriptor.getAttributeValue("implementation") + "\n");

            Element threadingProfile = descriptor.getChild("threading-profile");
            if (threadingProfile != null) {
                caption.append("maxBufferSize = " + threadingProfile.getAttributeValue("maxBufferSize") + "\n");
                caption.append("threadTTL = " + threadingProfile.getAttributeValue("threadTTL") + "\n");
                caption.append("maxThreadsActive = " + threadingProfile.getAttributeValue("maxThreadsActive") + "\n");
                caption.append("maxThreadsIdle = " + threadingProfile.getAttributeValue("maxThreadsIdle") + "\n");
            }
            Element poolingProfile = descriptor.getChild("pooling-profile");
            if (threadingProfile != null) {
                caption.append("exhaustedAction = " + poolingProfile.getAttributeValue("exhaustedAction") + "\n");
                caption.append("maxActive = " + poolingProfile.getAttributeValue("maxActive") + "\n");
                caption.append("maxIdle = " + poolingProfile.getAttributeValue("maxIdle") + "\n");
                caption.append("maxWait = " + poolingProfile.getAttributeValue("maxWait") + "\n");
            }
            appendProperties(descriptor, caption);
            appendDescription(descriptor, caption);

            node.getInfo().setCaption(caption.toString());

            processShortestNotation(graph, descriptor, node);

            processExceptionStrategy(graph, descriptor, node);

            processInboundRouters(graph, descriptor, node);

            processOutBoundRouters(graph, descriptor, node);

            processResponseRouter(graph, descriptor, node);

        }

    }

    private void processExceptionStrategy(Graph graph, Element descriptor,
                                          GraphNode node) {
        Element exceptionStrategy = descriptor.getChild("catch-all-strategy");
        if (exceptionStrategy == null)
            exceptionStrategy = descriptor.getChild("exception-strategy");

        if (exceptionStrategy != null) {

            String className = exceptionStrategy.getAttributeValue("className");
            GraphNode exceptionNode = graph.addNode();
            exceptionNode.getInfo().setCaption(className);
            exceptionNode.getInfo().setFillColor("indianred1");

            graph.addEdge(node, exceptionNode).getInfo().setCaption("catch-all-strategy");

            Element endpoint = exceptionStrategy.getChild(TAG_ENDPOINT);
            if (endpoint != null) {
                String url = endpoint.getAttributeValue(TAG_ATTRIBUTE_ADDRESS);
                if (url != null) {
                    GraphNode out = (GraphNode)getEndpoint(url, node.getInfo().getHeader());
                    if (out == null) {
                        out = graph.addNode();
                        out.getInfo().setCaption(url);
                        endpoints.put(url, out);
                    }
                    graph.addEdge(exceptionNode, out);
                }
            }
        }

    }

    private void processShortestNotation(Graph graph, Element descriptor,
                                         GraphNode node) {
        String inbound = descriptor.getAttributeValue("inboundEndpoint");
        if (inbound != null) {
            GraphNode in = (GraphNode)getEndpoint(inbound, node.getInfo().getHeader());
            if (in == null) {
                in = graph.addNode();
                in.getInfo().setCaption(inbound);
                endpoints.put(inbound, in);
            }
            graph.addEdge(in, node).getInfo().setCaption("in");
        }
        String outbound = descriptor.getAttributeValue("outboundEndpoint");
        if (outbound != null) {
            GraphNode out = (GraphNode)getEndpoint(outbound, node.getInfo().getHeader());
            if (out == null) {
                out = graph.addNode();
                out.getInfo().setCaption(outbound);
                endpoints.put(outbound, out);
            }
            graph.addEdge(node, out).getInfo().setCaption("out");
        }

        String inboundTransformers = descriptor
                .getAttributeValue("inboundTransformer");
        if (inboundTransformers != null) {
            String[] transformers = inboundTransformers.split(" ");
            StringBuffer caption = new StringBuffer();
            for (int i = 0; i < transformers.length; i++) {
                caption.append("transformer " + i + " : " + transformers[i]
                        + "\n");
            }
            node.getInfo().setCaption(caption.toString());
        }

        GraphNode[] virtual = getVirtualEndpoint(node.getInfo().getHeader());
        if(virtual.length > 0) {
            for (int i = 0; i < virtual.length; i++) {
                graph.addEdge(node, virtual[i]).getInfo().setCaption("out (dynamic)");
            }
        }
    }

    private void processResponseRouter(Graph graph, Element descriptor,
                                       GraphNode node) {
        Element responseRouterElement = descriptor.getChild("response-router");
        if (responseRouterElement != null) {

            Element router = responseRouterElement.getChild("router");
            String className = router.getAttributeValue("className");
            GraphNode responseRouter = graph.addNode();
            responseRouter.getInfo().setFillColor(COLOR_ROUTER);
            responseRouter.getInfo().setCaption(className);
            graph.addEdge(responseRouter, node).getInfo().setCaption("response-router");
            Element endpoint = responseRouterElement.getChild(TAG_ENDPOINT);
            String endpointAdress = endpoint.getAttributeValue(TAG_ATTRIBUTE_ADDRESS);
            GraphNode out = (GraphNode)getEndpoint(endpointAdress, node.getInfo().getHeader());
            graph.addEdge(out, responseRouter).getInfo().setCaption("in");
        }
    }

    private void processOutBoundRouters(Graph graph, Element descriptor,
                                        GraphNode node) {
        Element outboundRouter = descriptor.getChild("outbound-router");

        if (outboundRouter != null) {
            String componentName = node.getInfo().getHeader();
            List routers = outboundRouter.getChildren("router");
            processExceptionStrategy(graph, outboundRouter, node);

            for (Iterator iterator = routers.iterator(); iterator.hasNext();) {
                Element router = (Element) iterator.next();

                if (router != null) {
                    GraphNode routerNode = graph.addNode();
                    routerNode.getInfo().setHeader(router.getAttributeValue("className"));
                    routerNode.getInfo().setFillColor(COLOR_ROUTER);
                    graph.addEdge(node, routerNode).getInfo().setCaption("outbound router");
                    //processFilter(graph, router, routerNode);
                    processOutBoundRouterEndpoints(graph, router, routerNode, componentName);
                    processReplyTOasElement(graph, router, routerNode, componentName);
                    proceeReplyTOasProperty(graph, router, routerNode, componentName);

                    GraphNode[] virtual = getVirtualEndpoint(componentName + "." + router.getAttributeValue("className"));
                    if(virtual.length > 0) {
                        for (int i = 0; i < virtual.length; i++) {
                            graph.addEdge(routerNode, virtual[i]).getInfo().setCaption("out (dynamic)");
                        }
                    }

                }
            }

            GraphNode[] virtual = getVirtualEndpoint(componentName);
            if(virtual.length > 0) {
                for (int i = 0; i < virtual.length; i++) {
                    graph.addEdge(node, virtual[i]).getInfo().setCaption("out (dynamic)");
                }
            }

        }
    }

    private void processOutBoundRouterEndpoints(Graph graph, Element router,
                                                GraphNode routerNode, String componentName) {
        List epList = router.getChildren(TAG_ENDPOINT);
        for (Iterator iterator = epList.iterator(); iterator.hasNext();) {
            Element outEndpoint = (Element) iterator.next();

            String url = outEndpoint.getAttributeValue(TAG_ATTRIBUTE_ADDRESS);
            if (url != null) {
                GraphNode out = (GraphNode)getEndpoint(url, componentName);
                if (out == null) {
                    out = graph.addNode();
                    StringBuffer caption = new StringBuffer();
                    caption.append(url).append("\n");
                    appendProperties(outEndpoint, caption);
                    appendDescription(outEndpoint, caption);
                    out.getInfo().setCaption(caption.toString());
                    endpoints.put(url, out);
                    processOutboundFilter(graph, outEndpoint, out, routerNode);
                } else {
                    graph.addEdge(routerNode, out).getInfo().setCaption("out");
                }
            }

            GraphNode[] virtual = getVirtualEndpoint(componentName);
            if(virtual.length > 0) {
                for (int i = 0; i < virtual.length; i++) {
                    graph.addEdge(routerNode, virtual[i]).getInfo().setCaption("out (dynamic)");
                }
            }
        }
    }

    private void processInboundFilter(Graph graph, Element endpoint, GraphNode endpointNode, GraphNode parent) {
        Element filter=endpoint.getChild("filter");
        boolean conditional = false;

        if (filter == null) {
            filter=endpoint.getChild("left-filter");
            conditional = filter!=null;
        }

        if (filter != null) {

            GraphNode filterNode = graph.addNode();
            filterNode.getInfo().setHeader(filter.getAttributeValue("className"));
            filterNode.getInfo().setFillColor(COLOR_FILTER);
            StringBuffer caption = new StringBuffer();
            appendProperties(filter, caption);
            filterNode.getInfo().setCaption(caption.toString());
            //this is a hack to pick up and/or filter conditions
            //really we need a nice recursive way of doing this
            if(conditional) {
                filter=endpoint.getChild("right-filter");
                GraphNode filterNode2 = graph.addNode();
                filterNode2.getInfo().setHeader(filter.getAttributeValue("className"));
                filterNode2.getInfo().setFillColor(COLOR_FILTER);
                StringBuffer caption2 = new StringBuffer();
                appendProperties(filter, caption2);
                filterNode2.getInfo().setCaption(caption2.toString());
                graph.addEdge(endpointNode, filterNode2).getInfo().setCaption("filters on");
            }
            processInboundFilter(graph, filter, filterNode, parent);

            graph.addEdge(endpointNode, filterNode).getInfo().setCaption("filters on");
        } else {
            graph.addEdge(endpointNode, parent).getInfo().setCaption("in");
        }
    }

    //todo doesn't currently support And/Or logic filters
    private void processOutboundFilter(Graph graph, Element endpoint, GraphNode endpointNode, GraphNode parent) {
        Element filter=endpoint.getChild("filter");
        if (filter == null) filter=endpoint.getChild("left-filter");
        if (filter == null) filter=endpoint.getChild("right-filter");

        if (filter != null) {
            GraphNode filterNode = graph.addNode();
            filterNode.getInfo().setHeader(filter.getAttributeValue("className"));
            filterNode.getInfo().setFillColor(COLOR_FILTER);
            StringBuffer caption = new StringBuffer();
            appendProperties(filter, caption);
            filterNode.getInfo().setCaption(caption.toString());
            processOutboundFilter(graph, filter, filterNode, parent);
            graph.addEdge(filterNode, endpointNode).getInfo().setCaption("filters on");
        } else {
            graph.addEdge(parent, endpointNode).getInfo().setCaption("out");
        }
    }

    private void processReplyTOasElement(Graph graph, Element router,
                                         GraphNode routerNode, String componentName) {
        Element replyToElement = router.getChild("reply-to");
        if (replyToElement != null) {
            String replyTo = replyToElement.getAttributeValue(TAG_ATTRIBUTE_ADDRESS);
            if (replyTo != null) {
                GraphNode out = (GraphNode)getEndpoint(replyTo, componentName);
                graph.addEdge(routerNode, out).getInfo().setCaption("sets");
            }
        }
    }

    private void proceeReplyTOasProperty(Graph graph, Element router,
                                         GraphNode routerNode, String componentName) {
        Element propertiesEl = router.getChild("properties");
        if (propertiesEl != null) {
            List properties = propertiesEl.getChildren("property");
            for (Iterator iterator = properties.iterator(); iterator.hasNext();) {
                Element property = (Element) iterator.next();
                String propertyName = property.getAttributeValue("name");
                if ("replyTo".equals(propertyName)) {
                    String replyTo = property.getAttributeValue("value");
                    if (replyTo != null) {
                        GraphNode out = (GraphNode)getEndpoint(replyTo, componentName);
                        graph.addEdge(routerNode, out).getInfo().setCaption("sets");
                    }
                }
            }
        }
    }

    private void processInboundRouters(Graph graph, Element descriptor,
                                       GraphNode node) {
        Element inboundRouter = descriptor.getChild("inbound-router");

        if (inboundRouter != null) {

            GraphNode endpointsLink = node;

            Element router = inboundRouter.getChild("router");
            if (router != null) {
                GraphNode routerNode = graph.addNode();
                routerNode.getInfo().setCaption(router.getAttributeValue("className"));
                routerNode.getInfo().setFillColor(COLOR_ROUTER);

                graph.addEdge(routerNode, node).getInfo().setCaption("inbound router");
                endpointsLink = routerNode;
            }

            List inbounEndpoints = inboundRouter.getChildren(TAG_ENDPOINT);
            for (Iterator iterator = inbounEndpoints.iterator(); iterator
                    .hasNext();) {
                Element inEndpoint = (Element) iterator.next();
                String url = inEndpoint.getAttributeValue(TAG_ATTRIBUTE_ADDRESS);
                if (url != null) {
                    GraphNode in = (GraphNode)getEndpoint(url, node.getInfo().getHeader());
                    StringBuffer caption = new StringBuffer();
                    if (in == null) {
                        in = graph.addNode();
                        in.getInfo().setFillColor(COLOR_ENDPOINT);
                        caption.append(url).append("\n");
                        appendProperties(inEndpoint, caption);
                        appendDescription(inEndpoint, caption);
                        in.getInfo().setCaption(caption.toString());
                    } else {
                        //rewrite the properties
                        //todo really we need a cleaner way of handling in/out endpoints between components
                        caption.append(url).append("\n");
                        appendProperties(inEndpoint, caption);
                        appendDescription(inEndpoint, caption);
                        in.getInfo().setCaption(caption.toString());
                    }

                    if (in != null) {
                        processInboundFilter(graph, inEndpoint, in, endpointsLink);
                    }
                }
            }

        }
    }

    protected void saveGraph(Graph graph, String filename, File outFolder) throws IOException {
        // output graph to *.gif
        final String dotFileName = outFolder + "\\" + filename + ".dot";
        final String gifFileName = outFolder + "\\" + filename + ".gif";
        final String exeFile = getSaveExecutable();
        System.out.println("Executing: " + exeFile);
        GRAPHtoDOTtoGIF.transform(graph, dotFileName, gifFileName, exeFile);

    }

    private void parseEndpointIdentifiers(Graph graph, Element root) {
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
            node.getInfo().setFillColor(COLOR_DEFINED_ENDPOINTS);
            String name = endpoint.getAttributeValue("name");

            node.getInfo().setHeader(name);
            node.getInfo().setCaption(endpoint.getAttributeValue("value"));
            endpoints.put(name, node);
        }
    }

    private void parseEndpoints(Graph graph, Element root) {
            Element globalEndpoints = root.getChild("global-endpoints");

            if (globalEndpoints == null) {
                System.out.println("no global-endpoints");
                return;
            }

            List namedChildren = globalEndpoints.getChildren("endpoint");

            for (Iterator iter = namedChildren.iterator(); iter.hasNext();) {
                Element endpoint = (Element) iter.next();
                GraphNode node = graph.addNode();
                node.getInfo().setFillColor(COLOR_DEFINED_ENDPOINTS);
                String name = endpoint.getAttributeValue("name");

                node.getInfo().setHeader(endpoint.getAttributeValue("address") + " (" + name + ")");
                StringBuffer caption = new StringBuffer();
                appendProperties(endpoint, caption);
                node.getInfo().setCaption(caption.toString());
                endpoints.put(name, node);

               //processFilter(graph, endpoint, node);
            }
        }

    private String getSaveExecutable() throws FileNotFoundException {
        if (config.getExecuteCommand() == null) {
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.startsWith("windows")) {
                File f = new File("win32/dot.exe");
                config.setExecuteCommand(f.getAbsolutePath());
            } else {
                throw new UnsupportedOperationException("Mule Graph currently only works on Windows");
            }
        }
        File f = new File(config.getExecuteCommand());
        if (!f.exists()) {
            throw new FileNotFoundException(f.getAbsolutePath());
        }
        return config.getExecuteCommand();
    }

    private GraphNode getEndpoint(String uri, String componentName) {
        GraphNode n = getEqualsMapping(uri, componentName);
        if(n==null) n = (GraphNode)endpoints.get(uri);
        if(n==null) {
            for (Iterator iterator = endpoints.keySet().iterator(); iterator.hasNext();) {
                String s = (String) iterator.next();
                if(s.startsWith(uri + "/" + componentName)) {
                    n = (GraphNode)endpoints.get(s);
                }
            }
        }
        return n;
    }

    protected GraphNode getEqualsMapping(String uri, String componentName) {
        String equalsMapping = config.getMappings().getProperty(uri + ".equals");
        if(equalsMapping!=null) {
            System.out.println("Mapping equivilent endpoint '" + equalsMapping + "' to '" + uri + "'");
            return getEndpoint(equalsMapping, componentName);
        }
        return null;
    }


    protected GraphNode[] getVirtualEndpoint(String componentName) {

        List nodesList = new ArrayList();
        String mappedUri = config.getMappings().getProperty(componentName);
        if(mappedUri!=null) {
            StringTokenizer stringTokenizer = new StringTokenizer(mappedUri, ",");
            while (stringTokenizer.hasMoreTokens()) {
                String s = stringTokenizer.nextToken();
                System.out.println("Mapping virtual endpoint '" + s + "' for component '" + componentName + "'");
                GraphNode n = getEndpoint(s, componentName);
                if(n!=null) nodesList.add(n);
            }
        }

        GraphNode[] nodes = null;
        if(nodesList.size() > 0) {
            nodes = new GraphNode[nodesList.size()];
            nodes = (GraphNode[])nodesList.toArray(nodes);
        } else {
            nodes = new GraphNode[]{};
        }
        return nodes;
    }

    public static void printUsage() {
        System.out.println("Mule Configuration Grapher");
        System.out.println("Generates  graphs for Mule configuration files");
        System.out.println("-----------------------------------------------");
        System.out.println("-files      A comma-seperated list of Mule configuration files (required)");
        System.out.println("-outputdir  The directory to write the generated graphs to. Defaults to the current directory (optional)");
        System.out.println("-exec       The executable file used for Graph generation. Defaults to ./win32/dot.exe (optional)");
        System.out.println("-caption    Default caption for the generated graphs. Defaults to the 'id' attribute in the config file (optional)");
        System.out.println("-?          Displays this help");
    }
}

