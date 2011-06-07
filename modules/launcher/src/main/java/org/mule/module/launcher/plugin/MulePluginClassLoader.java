/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.plugin;

import java.net.URL;
import java.util.Set;

/**
 *
 */
public class MulePluginClassLoader extends FineGrainedControlClassLoader
{

    public MulePluginClassLoader(URL[] urls, ClassLoader parent)
    {
        super(urls, parent);
    }

    public MulePluginClassLoader(URL[] urls, ClassLoader parent, Set<String> overrides)
    {
        super(urls, parent, overrides);
    }
}
