package org.mule.tools.config.graph.postgraphers;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.util.Arrays;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogSystem;
import org.mule.tools.config.graph.components.PostGrapher;
import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.util.VelocityLogger;

public abstract class AbstractIndexer implements LogSystem, PostGrapher{

	private static VelocityEngine ve;

	static {
		ve = new VelocityEngine();
		ve.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM,
				new VelocityLogger());
		try {
			ve.init();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected void doRendering(GraphConfig config, File[] htmlFiles, String template, String targetFile) {
		try {

			VelocityContext velocityContext = new VelocityContext();

			velocityContext.put("fileList", Arrays.asList(htmlFiles));
			// TODO how to retrieve template using classpath ?
			Template t = ve
					.getTemplate(template);
			File file = new File(config.getOutputDirectory() + targetFile);
			FileWriter writer = new FileWriter(file);

			System.out.println("generating " + file);

			t.merge(velocityContext, writer);
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	protected File[] getFiles(final GraphConfig config, final String extension) {
		File[] htmlFiles = config.getOutputDirectory().listFiles(
				new FilenameFilter() {
					public boolean accept(File dir, String name) {
						if (name.toLowerCase().equals("index.html"))
							return false;
						else if (name.toLowerCase().equals("gallery.html"))
							return false;
						else
							return name.endsWith(extension);
					}
				});
		return htmlFiles;
	}
	
	public void init(RuntimeServices arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void logVelocityMessage(int arg0, String arg1) {
		System.out.println(arg1);
		
	}

	public abstract void postGrapher(GraphConfig config);
	
}
