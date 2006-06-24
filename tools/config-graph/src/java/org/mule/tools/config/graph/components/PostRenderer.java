/*
 * $Id: $
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.tools.config.graph.components;

import com.oy.shared.lm.graph.Graph;

import java.util.Map;

import org.mule.tools.config.graph.config.GraphEnvironment;

public interface PostRenderer
{

    public abstract void postRender(GraphEnvironment env, Map context, Graph graph);

}