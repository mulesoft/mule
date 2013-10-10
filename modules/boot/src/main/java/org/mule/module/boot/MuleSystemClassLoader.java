/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
