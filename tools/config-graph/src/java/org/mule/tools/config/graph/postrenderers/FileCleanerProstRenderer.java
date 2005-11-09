package org.mule.tools.config.graph.postrenderers;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;

import org.mule.tools.config.graph.components.PostRenderer;
import org.mule.tools.config.graph.config.GraphConfig;

import com.oy.shared.lm.graph.Graph;

public class FileCleanerProstRenderer implements PostRenderer{

	public void postRender(GraphConfig config, Map context,Graph graph) {
		if (!config.isKeepDotFiles()) {
			File[] dotFiles = config.getOutputDirectory().listFiles(
					new FilenameFilter() {
						public boolean accept(File dir, String name) {
							return name.endsWith(".dot") | name.endsWith(".cmapx");
						}
					});
			for (int x = 0; x < dotFiles.length; x++) {
				dotFiles[x].delete();
			}
		}		
	}

}
