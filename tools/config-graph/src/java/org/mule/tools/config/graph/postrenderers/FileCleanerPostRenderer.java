package org.mule.tools.config.graph.postrenderers;

import com.oy.shared.lm.graph.Graph;
import org.mule.tools.config.graph.components.PostRenderer;
import org.mule.tools.config.graph.config.GraphEnvironment;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Map;

public class FileCleanerPostRenderer implements PostRenderer{

	public void postRender(GraphEnvironment env, Map context,Graph graph) {
		if (!env.getConfig().isKeepDotFiles()) {
			File[] dotFiles = env.getConfig().getOutputDirectory().listFiles(
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
