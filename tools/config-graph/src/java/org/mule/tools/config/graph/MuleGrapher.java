package org.mule.tools.config.graph;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphFactory;
import com.oy.shared.lm.graph.GraphNode;
import com.oy.shared.lm.out.GRAPHtoDOTtoGIF;

public class MuleGrapher {
    // colors http://www.graphviz.org/pub/scm/graphviz2/doc/info/colors.html

    private static final String TAG_ATTRIBUTE_ADDRESS = "address";
    private static final String TAG_ENDPOINT = "endpoint";
    private static final String COLOR_DEFINED_ENDPOINTS = "lightblue";
    private static final String COLOR_ROUTER = "darkolivegreen3";

    private Map endpoints = new HashMap();
    private String file = "";
    private static String exec = null;

    public static void main(String[] args) throws IOException, JDOMException {

        if(args.length==0 || args[0].equals("?")) {
            printUsage();
            System.exit(0);
        }

        String files = getOpt(args, "-files", null);

        String outputDir = getOpt(args, "-outputdir", ".");
        File f = new File(outputDir);
        if(!f.exists()) f.mkdirs();
        System.out.println("Outputting graphs to: " + f.getAbsolutePath());

        String caption = getOpt(args, "-caption", null);

        exec = getOpt(args, "-exec", null);

        if (files == null) {
            System.out.println("-files arg not set");
            System.exit(1);
        }

        MuleGrapher grapher = null;
        for (StringTokenizer stringTokenizer = new StringTokenizer(files, ","); stringTokenizer.hasMoreTokens();) {
            String s = stringTokenizer.nextToken().trim();
            grapher = new MuleGrapher(s);
            grapher.generateGraph(outputDir, caption);
        }


    }

    private static String getOpt(String[] args, String name, String defaultValue) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals(name)) {
                if (i + 1 >= args.length) {
                    return defaultValue;
                } else {
                    String arg = args[i + 1];
                    if(arg.startsWith("-")){
                        return defaultValue;
                    } else {
                        return arg;
                    }
                }
            }
        }
        return defaultValue;
    }


    public MuleGrapher(String file) {
        this.file = file;
    }

    public Graph generateGraph(String outputDir, String caption) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        builder.setValidation(true);
        builder.setIgnoringElementContentWhitespace(true);
        File myFile = new File(file);
        Document doc = builder.build(myFile);
        System.out.println("processing file " + myFile.getCanonicalPath());

        Graph graph = GraphFactory.newGraph();


        Element root = doc.getRootElement();

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
        parseEndpoints(graph, root);

        Element model = root.getChild("model");
        if (model != null) {
            parseModel(graph, model);

        } else {
            parseModel(graph, root);

        }
        parseConnectors(graph, root);
        saveGraph(graph, outputDir);

        return graph;
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

    private void appendProperties(Element connector, StringBuffer caption) {
        Element properties = connector.getChild("properties");
        if (properties != null) {
            for (Iterator iterator = properties.getChildren("property")
                    .iterator(); iterator.hasNext();) {
                Element property = (Element) iterator.next();
                caption.append(property.getAttributeValue("name") + " :"
                        + property.getAttributeValue("value") + "\n");
            }
        }
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
                    GraphNode out = (GraphNode) endpoints.get(url);
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
            GraphNode in = (GraphNode) endpoints.get(inbound);
            if (in == null) {
                in = graph.addNode();
                in.getInfo().setCaption(inbound);
                endpoints.put(inbound, in);
            }
            graph.addEdge(in, node).getInfo().setCaption("in");
        }
        String outbound = descriptor.getAttributeValue("outboundEndpoint");
        if (outbound != null) {
            GraphNode out = (GraphNode) endpoints.get(outbound);
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
            GraphNode out = (GraphNode) endpoints.get(endpointAdress);
            graph.addEdge(out, responseRouter).getInfo().setCaption("in");
        }
    }

    private void processOutBoundRouters(Graph graph, Element descriptor,
                                        GraphNode node) {
        Element outboundRouter = descriptor.getChild("outbound-router");

        if (outboundRouter != null) {
            List routers = outboundRouter.getChildren("router");
            processExceptionStrategy(graph, outboundRouter, node);

            for (Iterator iterator = routers.iterator(); iterator.hasNext();) {
                Element router = (Element) iterator.next();

                if (router != null) {
                    GraphNode routerNode = graph.addNode();
                    routerNode.getInfo().setHeader(router.getAttributeValue("className"));
                    routerNode.getInfo().setFillColor(COLOR_ROUTER);
                    graph.addEdge(node, routerNode).getInfo().setCaption("outbound router");
                    processFilter(router, routerNode);
                    processOutBoundRouterEndpoints(graph, router, routerNode);
                    processReplyTOasElement(graph, router, routerNode);
                    proceeReplyTOasProperty(graph, router, routerNode);

                }
            }

        }
    }

    private void processOutBoundRouterEndpoints(Graph graph, Element router,
                                                GraphNode routerNode) {
        List epList = router.getChildren(TAG_ENDPOINT);
        for (Iterator iterator = epList.iterator(); iterator.hasNext();) {
            Element outEndpoint = (Element) iterator.next();

            String url = outEndpoint.getAttributeValue(TAG_ATTRIBUTE_ADDRESS);
            if (url != null) {
                GraphNode out = (GraphNode) endpoints.get(url);
                if (out == null) {
                    out = graph.addNode();
                    StringBuffer caption = new StringBuffer();
                    caption.append(url).append("\n");
                    appendProperties(outEndpoint, caption);
                    appendDescription(outEndpoint, caption);
                    out.getInfo().setCaption(caption.toString());
                    endpoints.put(url, out);
                }

                graph.addEdge(routerNode, out).getInfo().setCaption("out");
            }
        }
    }

    private void processFilter(Element router, GraphNode routerNode) {
        if (router.getChild("filter") != null) {
            String filterCaptions = "expectedType : "
                    + router.getChild("filter").getAttributeValue("expectedType") + "\n";
            filterCaptions += "className : "
                    + router.getChild("filter").getAttributeValue("className")
                    + "\n";
            filterCaptions += "expression : "
                    + router.getChild("filter").getAttributeValue("expression");
            routerNode.getInfo().setCaption(filterCaptions);
        }
    }

    private void processReplyTOasElement(Graph graph, Element router,
                                         GraphNode routerNode) {
        Element replyToElement = router.getChild("reply-to");
        if (replyToElement != null) {
            String replyTo = replyToElement.getAttributeValue(TAG_ATTRIBUTE_ADDRESS);
            if (replyTo != null) {
                GraphNode out = (GraphNode) endpoints.get(replyTo);
                graph.addEdge(routerNode, out);
            }
        }
    }

    private void proceeReplyTOasProperty(Graph graph, Element router,
                                         GraphNode routerNode) {
        Element propertiesEl = router.getChild("properties");
        if (propertiesEl != null) {
            List properties = propertiesEl.getChildren("property");
            for (Iterator iterator = properties.iterator(); iterator.hasNext();) {
                Element property = (Element) iterator.next();
                String propertyName = property.getAttributeValue("name");
                if ("replyTo".equals(propertyName)) {
                    String replyTo = property.getAttributeValue("value");
                    if (replyTo != null) {
                        GraphNode out = (GraphNode) endpoints.get(replyTo);
                        graph.addEdge(routerNode, out);
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
                Element endpointIn = (Element) iterator.next();
                String url = endpointIn.getAttributeValue(TAG_ATTRIBUTE_ADDRESS);
                if (url != null) {
                    GraphNode in = (GraphNode) endpoints.get(url);
                    if (in == null) {
                        in = graph.addNode();
                        StringBuffer caption = new StringBuffer();
                        caption.append(url).append("\n");
                        appendProperties(endpointIn, caption);
                        appendDescription(endpointIn, caption);
                        in.getInfo().setCaption(caption.toString());
                    }
                    if (in != null)
                        graph.addEdge(in, endpointsLink).getInfo().setCaption("in");
                }
            }

        }
    }

    protected void saveGraph(Graph graph, String outFolder) throws IOException {
        // output graph to *.gif
        String name = new File(file).getName();
        final String dotFileName = outFolder + "\\" + name + ".dot";
        final String gifFileName = outFolder + "\\" + name + ".gif";
        final String exeFile = getSaveExecutable();
        System.out.println("Executing: " + exeFile);
        GRAPHtoDOTtoGIF.transform(graph, dotFileName, gifFileName, exeFile);

    }

    private void parseEndpoints(Graph graph, Element root) {
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


    private String getSaveExecutable() throws FileNotFoundException {
        if (exec == null) {
            String osName = System.getProperty("os.name").toLowerCase();
            if (osName.startsWith("windows")) {
                File f = new File("./win32/dot.exe");
                exec = f.getAbsolutePath();
            } else {
                throw new UnsupportedOperationException("Mule Graph currently only works on Windows");
            }
        }
        File f = new File(exec);
        if (!f.exists()) {
            throw new FileNotFoundException(f.getAbsolutePath());
        }
        return exec;
    }

    public static void printUsage() {
        System.out.println("Mule Configuration Grapher");
        System.out.println("Generates  graphs for Mule configuration files");
        System.out.println("-----------------------------------------------");
        System.out.println("-files      A comma-seperated list of Mule configuration files (required)");
        System.out.println("-outputdir  The directory to write the generated graphs to. Defaults to the current directory (optional)");
        System.out.println("-exec       The executable file used for Graph generation. Defaults to ./win32/dot.exe (optional)");
        System.out.println("-caption    Default caption for the generated graphs. Defaults to the 'id' attribute in the config file (optional)");
        System.out.println("?           Displays this help");
    }
}

