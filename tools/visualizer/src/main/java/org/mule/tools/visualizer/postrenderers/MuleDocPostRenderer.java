/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.visualizer.postrenderers;

import org.mule.tools.visualizer.components.PostRenderer;
import org.mule.tools.visualizer.config.GraphEnvironment;
import org.mule.tools.visualizer.config.VelocitySupport;

import com.oy.shared.lm.graph.Graph;

import java.io.File;
import java.io.FileWriter;
import java.util.Iterator;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;

public class MuleDocPostRenderer extends VelocitySupport implements PostRenderer
{

    public static final String DEFAULT_MULE_TEMPLATE = "template/mule-config.vm";

    private String template;

    public MuleDocPostRenderer(GraphEnvironment env) throws Exception
    {
        super(env);
        template = env.getProperties().getProperty("muleDocTemplate");
        if (template == null)
        {
            template = DEFAULT_MULE_TEMPLATE;
        }
    }
    
    public void postRender(GraphEnvironment env, Map context, Graph graph)
    {
        try
        {

            VelocityContext velocityContext = new VelocityContext();

            for (Iterator iter = context.keySet().iterator(); iter.hasNext();)
            {
                String key = (String) iter.next();
                String value = (String) context.get(key);
                velocityContext.put(key, value);
            }

            velocityContext.put("graph", graph);
            Template t = getVe().getTemplate(template);
            File file = new File(context.get("htmlFileName").toString());
            FileWriter writer = new FileWriter(file);

            env.log("generating " + file);

            t.merge(velocityContext, writer);
            writer.flush();
            writer.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
