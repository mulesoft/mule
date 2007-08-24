/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.visualizer.components;

import org.mule.tools.visualizer.config.GraphEnvironment;

import com.oy.shared.lm.graph.Graph;

import java.util.Map;

public interface PostRenderer
{

    void postRender(GraphEnvironment env, Map context, Graph graph);

}
