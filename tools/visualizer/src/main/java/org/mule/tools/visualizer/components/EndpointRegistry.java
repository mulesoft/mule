/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.visualizer.components;

import org.mule.tools.visualizer.config.GraphEnvironment;

import com.oy.shared.lm.graph.GraphNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class EndpointRegistry
{

    private Map endpoints = null;
    private GraphEnvironment env;

    public EndpointRegistry(GraphEnvironment env)
    {
        this.env = env;
        endpoints = new HashMap();
    }

    public GraphNode[] getVirtualEndpoint(String componentName)
    {

        List nodesList = new ArrayList();
        String mappedUri = env.getConfig().getMappings().getProperty(componentName);
        if (mappedUri != null)
        {
            StringTokenizer stringTokenizer = new StringTokenizer(mappedUri, ",");
            while (stringTokenizer.hasMoreTokens())
            {
                String s = stringTokenizer.nextToken();
                env.log("Mapping virtual endpoint '" + s + "' for component '" + componentName + "'");
                GraphNode n = getEndpoint(s, componentName);
                if (n != null)
                {
                    nodesList.add(n);
                }
            }
        }

        GraphNode[] nodes = null;
        if (nodesList.size() > 0)
        {
            nodes = new GraphNode[nodesList.size()];
            nodes = (GraphNode[]) nodesList.toArray(nodes);
        }
        else
        {
            nodes = new GraphNode[]{};
        }
        return nodes;
    }

    public GraphNode getEndpoint(String uri, String componentName)
    {
        GraphNode n = getEqualsMapping(uri, componentName);
        if (n == null)
        {
            n = (GraphNode) endpoints.get(uri);
        }
        if (n == null)
        {
            for (Iterator iterator = endpoints.keySet().iterator(); iterator.hasNext();)
            {
                String s = (String) iterator.next();
                if (s.startsWith(uri + "/" + componentName))
                {
                    n = (GraphNode) endpoints.get(s);
                }
            }
        }

        return n;
    }

    private GraphNode getEqualsMapping(String uri, String componentName)
    {
        String equalsMapping = env.getConfig().getMappings().getProperty(uri + ".equals");
        if (equalsMapping != null)
        {
            env.log("Mapping equivilent endpoint '" + equalsMapping + "' to '" + uri + "'");
            return getEndpoint(equalsMapping, componentName);
        }
        return null;
    }

    public void addEndpoint(String url, GraphNode out)
    {
        endpoints.put(url, out);
    }
}
