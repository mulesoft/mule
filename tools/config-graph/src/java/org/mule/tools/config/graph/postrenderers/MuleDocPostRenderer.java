package org.mule.tools.config.graph.postrenderers;

import com.oy.shared.lm.graph.Graph;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.mule.tools.config.graph.components.PostRenderer;
import org.mule.tools.config.graph.config.GraphEnvironment;
import org.mule.tools.config.graph.config.VelocitySupport;

import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Map;

public class MuleDocPostRenderer extends VelocitySupport implements PostRenderer {

    public MuleDocPostRenderer(GraphEnvironment env) {
        super(env);
    }

	public void postRender(GraphEnvironment env, Map context, Graph graph) {
		
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
			File file = new File(env.getConfig().getOutputDirectory() + "/"
					+ context.get("htmlFileName"));
			FileWriter writer = new FileWriter(file);

			env.log("generating " + file);

			t.merge(velocityContext, writer);
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
	

	
