package org.mule.tools.config.graph.postgraphers;

import java.io.File;

import org.mule.tools.config.graph.config.GraphConfig;

public class DocIndexerPostGrapher extends AbstractIndexer {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.tools.config.graph.PostGrapher#postGrapher(org.mule.tools.config.graph.GraphConfig)
	 */
	public void postGrapher(final GraphConfig config) {
		File[] htmlFiles = getFiles(config,".html");
		// TODO no more hardcoded template path
		String template = "./src/resources/template/doc-index.vm";
		String targetFile = "/index.html";
		doRendering(config, htmlFiles, template, targetFile);

	}

	public String getStatusTitle() {	
		return "Generating Index page";
	}



}
