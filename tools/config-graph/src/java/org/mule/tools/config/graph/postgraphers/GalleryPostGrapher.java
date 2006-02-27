package org.mule.tools.config.graph.postgraphers;

import org.mule.tools.config.graph.config.GraphEnvironment;

import java.io.File;

public class GalleryPostGrapher extends AbstractIndexer{

    public GalleryPostGrapher(GraphEnvironment env) {
        super(env);
    }

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.tools.config.graph.PostGrapher#postGrapher(org.mule.tools.config.graph.GraphConfig)
	 */
	public void postGrapher(GraphEnvironment env) {
		File[] htmlFiles = getFiles(env.getConfig(),".gif");
		// TODO no more hardcoded template path
		String template = "./src/resources/template/gallery-index.vm";
		String targetFile = "/gallery.html";
		doRendering(env, htmlFiles, template, targetFile);

	}
	public String getStatusTitle() {	
		return "Generating Gallery page";
	}
}
