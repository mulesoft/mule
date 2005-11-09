package org.mule.tools.config.graph.postrenderers;

import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.mule.tools.config.graph.components.PostRenderer;
import org.mule.tools.config.graph.config.GraphConfig;
import org.mule.tools.config.graph.util.VelocityLogger;

import com.oy.shared.lm.graph.Graph;

public class MuleDocPostRenderer implements PostRenderer {

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

	public void postRender(GraphConfig config, Map context, Graph graph) {
		
		try {

			VelocityContext velocityContext = new VelocityContext();

			for (Iterator iter = context.keySet().iterator(); iter.hasNext();) {
				String key = (String) iter.next();
				String value = (String) context.get(key);
				velocityContext.put(key, value);
			}
			
			velocityContext.put("graph",graph);
			
			// TODO how to retrieve template using classpath ?
			Template t = ve
					.getTemplate("./src/resources/template/mule-config.vm");
			File file = new File(config.getOutputDirectory() + "/"
					+ context.get("htmlFileName"));
			FileWriter writer = new FileWriter(file);

			System.out.println("generating " + file);

			t.merge(velocityContext, writer);
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
	

	
