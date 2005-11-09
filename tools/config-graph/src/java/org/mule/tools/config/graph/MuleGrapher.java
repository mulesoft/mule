package org.mule.tools.config.graph;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.mule.config.MuleDtdResolver;
import org.mule.tools.config.graph.components.GraphRenderer;
import org.mule.tools.config.graph.components.MuleParser;
import org.mule.tools.config.graph.components.PostGrapher;
import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.postgraphers.DocIndexerPostGrapher;
import org.mule.tools.config.graph.postgraphers.GalleryPostGrapher;
import org.mule.tools.config.graph.postgraphers.MediaCopierPostGrapher;
import org.mule.tools.config.graph.processor.TagProcessor;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphFactory;

public class MuleGrapher extends TagProcessor {

	private final GraphConfig config;

	private final GraphRenderer graphRenderer;

	private final List postGraphers = new ArrayList();

	public static void main(String[] args) {

		if (args.length == 0 || args[0].equals(GraphConfig.ARG_HELP)) {
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
		super(config);
		this.config = config;
		this.graphRenderer = new GraphRenderer(config);
		this.postGraphers.add(new DocIndexerPostGrapher());
		this.postGraphers.add(new GalleryPostGrapher());
		this.postGraphers.add(new MediaCopierPostGrapher());

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
			if (config.isCombineFiles()) {
				if (filename == null)
					filename = config.getFiles().get(0).toString() + ".combined";
				generateGraph(1, config.getFiles(), config.getOutputDirectory(), config.getCaption(), filename);
			} else {
				int ind = 0;
				for (Iterator iterator = config.getFiles().iterator(); iterator.hasNext();) {
					ind++;
					String s = (String) iterator.next();
					List list = new ArrayList(1);
					list.add(s);
					generateGraph(ind, list, config.getOutputDirectory(), config.getCaption(), new File(s).getName());
				}
			}

			for (Iterator iter = postGraphers.iterator(); iter.hasNext();) {
				PostGrapher postGrapher = (PostGrapher) iter.next();
				System.out.println("************ " + postGrapher.getStatusTitle());
				postGrapher.postGrapher(config);
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	protected void generateGraph(int i, List files, File outputDir, String caption, String fileName)
			throws JDOMException, IOException {
		SAXBuilder builder = new SAXBuilder();
		builder.setValidation(true);
		builder.setEntityResolver(new MuleDtdResolver());
		Graph graph = GraphFactory.newGraph();

		builder.setIgnoringElementContentWhitespace(true);
		for (Iterator iterator = files.iterator(); iterator.hasNext();) {

			String s = (String) iterator.next();
			File myFile = new File(s);
			System.out.println("**************** processing " + i + " of " + files.size() + 1 + " : "
					+ myFile.getCanonicalPath());
			MuleParser muleParser = new MuleParser(config, builder);
			muleParser.parseMuleConfig(myFile, graph);
			if (files.size() > 1) {
				if (caption == null)
					caption = "(no caption set)";
				graph.getInfo().setCaption(caption);
			}
			graphRenderer.saveGraph(graph, fileName, outputDir);

		}

	}

	public static void printUsage() {
		System.out.println("Mule Configuration Grapher");
		System.out.println("Generates  graphs for Mule configuration files");
		System.out.println("-----------------------------------------------");
		System.out.println("-files      A comma-seperated list of Mule configuration files (required)");
		System.out
				.println("-outputdir  The directory to write the generated graphs to. Defaults to the current directory (optional)");
		System.out
				.println("-exec       The executable file used for Graph generation. Defaults to ./win32/dot.exe (optional)");
		System.out
				.println("-caption    Default caption for the generated graphs. Defaults to the 'id' attribute in the config file (optional)");
		System.out.println("-?          Displays this help");
	}
}
