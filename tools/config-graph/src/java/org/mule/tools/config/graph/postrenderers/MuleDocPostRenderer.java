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

    public static final String DEFAULT_MULE_TEMPLATE = "./src/resources/template/mule-config.vm";

    protected String template;
    public MuleDocPostRenderer(GraphEnvironment env) throws Exception {
        super(env);
        template = env.getProperties().getProperty("muleDocTemplate");
        if(template==null)  {
            template = DEFAULT_MULE_TEMPLATE;
        }
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
            Template t = ve.getTemplate(template);
            File file = new File(context.get("htmlFileName").toString());
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



