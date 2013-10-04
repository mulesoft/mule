/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.boot;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Loads a standard $MULE_HOME/lib/* hierarchy.
 */
public class MuleSystemClassLoader extends URLClassLoader
{

    protected transient Log logger = LogFactory.getLog(getClass());

    public MuleSystemClassLoader()
    {
        super(new URL[0]);
        try
        {
            DefaultMuleClassPathConfig classPath = new DefaultMuleClassPathConfig(MuleBootstrap.lookupMuleHome(),
                                                                                  MuleBootstrap.lookupMuleBase());

            final List<URL> urlsList = classPath.getURLs();
            for (URL url : urlsList)
            {
                addURL(url);
            }
        }
        catch (Exception e)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(e);
            }
        }
    }
}
