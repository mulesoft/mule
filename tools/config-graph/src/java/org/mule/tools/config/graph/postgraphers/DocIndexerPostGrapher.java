package org.mule.tools.config.graph.postgraphers;

import org.mule.tools.config.graph.config.GraphEnvironment;

import java.io.File;

public class DocIndexerPostGrapher extends AbstractIndexer {

    public DocIndexerPostGrapher(GraphEnvironment env) {
        super(env);
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.tools.config.graph.PostGrapher#postGrapher(org.mule.tools.config.graph.GraphConfig)
	 */
	public void postGrapher(GraphEnvironment env) {
		File[] htmlFiles = getFiles(env.getConfig(),".html");
		// TODO no more hardcoded template path
		String template = "./src/resources/template/doc-index.vm";
		String targetFile = "/index.html";
		doRendering(env, htmlFiles, template, targetFile);

	}

	public String getStatusTitle() {	
		return "Generating Index page";
	}



}
