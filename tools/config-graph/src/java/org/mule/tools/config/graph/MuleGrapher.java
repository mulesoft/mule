package org.mule.tools.config.graph;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphFactory;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.mule.config.MuleDtdResolver;
import org.mule.tools.config.graph.components.EndpointRegistry;
import org.mule.tools.config.graph.components.GraphRenderer;
import org.mule.tools.config.graph.components.MuleParser;
import org.mule.tools.config.graph.components.PostGrapher;
import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.config.GraphEnvironment;
import org.mule.tools.config.graph.postgraphers.DocIndexerPostGrapher;
import org.mule.tools.config.graph.postgraphers.GalleryPostGrapher;
import org.mule.tools.config.graph.postgraphers.MediaCopierPostGrapher;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MuleGrapher {

    private GraphEnvironment env;

    private final GraphRenderer graphRenderer;

    private final List postGraphers = new ArrayList();

    public static void main(String[] args) {

        if (args.length == 0 || args[0].equals(GraphConfig.ARG_HELP)) {
            printUsage();
            System.exit(0);
        }
        MuleGrapher grapher = null;
        GraphEnvironment env = null;
        try {
            env = new GraphConfig().init(args);
            grapher = new MuleGrapher(env);
        } catch (Exception e) {
            env.logError("MuleGrapher failed to process: " + e.getMessage(), e);
            System.exit(0);
        }
        grapher.run();
    }

    public MuleGrapher(GraphEnvironment environment) throws Exception {
        env = environment;
        this.graphRenderer = new GraphRenderer(env);
        this.postGraphers.add(new DocIndexerPostGrapher(env));
        this.postGraphers.add(new GalleryPostGrapher(env));
        this.postGraphers.add(new MediaCopierPostGrapher());

    }

    public void run() {
        try {
            env.getConfig().validate();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            System.exit(0);
        }
        try {
            String filename = env.getConfig().getOutputFilename();
            if (env.getConfig().isCombineFiles()) {
                generateIndividual();
                generateCombined(filename);
            } else {
                generateIndividual();
            }

            for (Iterator iter = postGraphers.iterator(); iter.hasNext();) {
                PostGrapher postGrapher = (PostGrapher) iter.next();
                env.log("************ " + postGrapher.getStatusTitle());
                postGrapher.postGrapher(env);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

    }

    protected void generateCombined(String filename) throws IOException, JDOMException {
        env.setDoingCombinedGeneration(true);
        env.setEndpointRegistry(new EndpointRegistry(env));
        env.log("Doing Combined Generation with file name: " + filename);
        if (filename == null) {
            filename = env.getConfig().getFiles().get(0).toString() + ".combined";
        }
        generateGraph(1, env.getConfig().getFiles(), env.getConfig().getOutputDirectory(), env.getConfig().getCaption(), filename);
    }

    protected void generateIndividual() throws IOException, JDOMException {
        env.setDoingCombinedGeneration(false);

        int ind = 0;
        for (Iterator iterator = env.getConfig().getFiles().iterator(); iterator.hasNext();) {
            env.setEndpointRegistry(new EndpointRegistry(env));
            ind++;
            String s = (String) iterator.next();
            List list = new ArrayList(1);
            list.add(s);
            env.log("Doing inividual generation for file: " + s);
            generateGraph(ind, list, env.getConfig().getOutputDirectory(), env.getConfig().getCaption(), new File(s).getName());
        }
    }


    protected void generateGraph(int i, List files, File outputDir, String caption, String fileName)
            throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        builder.setValidation(true);
        builder.setEntityResolver(new MuleDtdResolver());
        Graph graph = GraphFactory.newGraph();

        builder.setIgnoringElementContentWhitespace(true);
        MuleParser muleParser = new MuleParser(env, builder);
        for (Iterator iterator = files.iterator(); iterator.hasNext();) {

            String s = (String) iterator.next();
            File myFile = new File(s);
            env.log("**************** processing " + i + " of " + files.size() + 1 + " : "
                    + myFile.getCanonicalPath());

            muleParser.parseMuleConfig(myFile, graph);
            if (files.size() > 1) {
                if (caption == null)
                    caption = "(no caption set)";
                graph.getInfo().setCaption(caption);
            }
            if (!env.getConfig().isCombineFiles()) {
                muleParser.finalise(graph);
                graphRenderer.saveGraph(graph, fileName, outputDir);
            }
        }
        if (env.getConfig().isCombineFiles()) {
            muleParser.finalise(graph);
            graphRenderer.saveGraph(graph, fileName, outputDir);
        }

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
