package org.mule.tools.config.graph.components;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.postrenderers.FileCleanerProstRenderer;
import org.mule.tools.config.graph.postrenderers.MuleDocPostRenderer;
import org.mule.tools.config.graph.util.DOTtoMAP;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.out.GRAPHtoDOTtoGIF;

public class GraphRenderer {

	private GraphConfig config;
	private List postRenderers= new ArrayList();
	
	public GraphRenderer(GraphConfig config) {
		this.config = config;
		postRenderers.add(new MuleDocPostRenderer());
		postRenderers.add(new FileCleanerProstRenderer());
		
	}

	public void saveGraph(Graph graph, String filename, File outFolder)
			throws IOException {
		// output graph to *.gif
		final String dotFileName = outFolder + "\\" + filename + ".dot";
		final String mapFileName = outFolder + "\\" + filename + ".cmapx";
		final String gifFileName = outFolder + "\\" + filename + ".gif";
		
		
		final String exeFile = getSaveExecutable();
		System.out.println("Executing: " + exeFile);
		GRAPHtoDOTtoGIF.transform(graph, dotFileName, gifFileName, exeFile);
		System.out.println("generating MAP");
		DOTtoMAP.transform(exeFile, dotFileName, mapFileName);

		Map context = new HashMap();
		context.put("dotFileName",filename + ".dot");
		context.put("mapFileName",mapFileName);
		context.put("gifFileName",filename + ".gif");
		context.put("htmlFileName",filename + ".html");
		context.put("outFolder",outFolder.getAbsolutePath());

		for (Iterator iter = postRenderers.iterator(); iter.hasNext();) {
			PostRenderer element = (PostRenderer) iter.next();
			element.postRender(config,context,graph);
		}
	}

	private String getSaveExecutable() throws FileNotFoundException {
		if (config.getExecuteCommand() == null) {
			String osName = System.getProperty("os.name").toLowerCase();
			if (osName.startsWith("windows")) {
				File f = new File("win32/dot.exe");
				config.setExecuteCommand(f.getAbsolutePath());
			} else {
				throw new UnsupportedOperationException(
						"Mule Graph currently only works on Windows");
			}
		}
		File f = new File(config.getExecuteCommand());
		if (!f.exists()) {
			throw new FileNotFoundException(f.getAbsolutePath());
		}
		return config.getExecuteCommand();
	}
}
