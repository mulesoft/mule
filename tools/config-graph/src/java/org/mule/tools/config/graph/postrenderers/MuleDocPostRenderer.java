/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.tools.config.graph.postrenderers;

import com.oy.shared.lm.graph.Graph;

import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.mule.tools.config.graph.components.PostRenderer;
import org.mule.tools.config.graph.config.GraphEnvironment;
import org.mule.tools.config.graph.config.VelocitySupport;

public class MuleDocPostRenderer extends VelocitySupport implements PostRenderer
{

    public static final String DEFAULT_MULE_TEMPLATE = "./src/resources/template/mule-config.vm";

    protected String template;

    public MuleDocPostRenderer(GraphEnvironment env) throws Exception
    {
        super(env);
        template = env.getProperties().getProperty("muleDocTemplate");
        if (template == null) {
            template = DEFAULT_MULE_TEMPLATE;
        }
    }

    public void postRender(GraphEnvironment env, Map context, Graph graph)
    {
        try {

            VelocityContext velocityContext = new VelocityContext();

            for (Iterator iter = context.keySet().iterator(); iter.hasNext();) {
                String key = (String)iter.next();
                String value = (String)context.get(key);
                velocityContext.put(key, value);
            }

            velocityContext.put("graph", graph);
            Template t = ve.getTemplate(template);
            File file = new File(context.get("htmlFileName").toString());
            FileWriter writer = new FileWriter(file);

            env.log("generating " + file);

            t.merge(velocityContext, writer);
            writer.flush();
            writer.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
