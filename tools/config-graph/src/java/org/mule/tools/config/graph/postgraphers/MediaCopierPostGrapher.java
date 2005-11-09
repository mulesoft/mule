package org.mule.tools.config.graph.postgraphers;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.mule.tools.config.graph.components.PostGrapher;
import org.mule.tools.config.graph.config.GraphConfig;

public class MediaCopierPostGrapher implements PostGrapher {

	public String getStatusTitle() {
		
		return "Copy Media files (logo, css,...)";
	}

	public void postGrapher(GraphConfig config) {

		try {
			FileUtils.copyDirectory(new File("./src/resources/media/"),config.getOutputDirectory());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		
		}
	}

}
