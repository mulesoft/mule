/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.hotdeploy;

import org.mule.module.boot.MuleBootstrapUtils;
import org.mule.util.FileUtils;
import org.mule.util.SystemUtils;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *  Load $MULE_HOME/lib/shared/<domain> libraries.
 */
public class MuleSharedDomainClassLoader extends URLClassLoader
{

    protected transient Log logger = LogFactory.getLog(getClass());

    private final String domain = "undefined";

    @SuppressWarnings("unchecked")
    public MuleSharedDomainClassLoader(String domain, ClassLoader parent)
    {
        super(new URL[0], parent);
        try
        {
            File domainDir = new File(MuleBootstrapUtils.getMuleHomeFile(), "lib/shared/" + domain);
            if (!domainDir.exists())
            {
                throw new IllegalArgumentException(
                        String.format("Shared ClassLoader Domain '%s' doesn't exist", domain));
            }

            if (!domainDir.canRead())
            {
                throw new IllegalArgumentException(
                        String.format("Shared ClassLoader Domain '%s' is not accessible", domain));
            }

            Collection<File> jars = FileUtils.listFiles(domainDir, new String[] {"jar"}, false);

            if (logger.isDebugEnabled())
            {
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Loading Shared ClassLoader Domain: ").append(domain).append(SystemUtils.LINE_SEPARATOR);
                    sb.append("=============================").append(SystemUtils.LINE_SEPARATOR);

                    for (File jar : jars)
                    {
                        sb.append(jar.toURI().toURL()).append(SystemUtils.LINE_SEPARATOR);
                    }

                    sb.append("=============================").append(SystemUtils.LINE_SEPARATOR);

                    logger.debug(sb.toString());
                }
            }

            for (File jar : jars)
            {
                addURL(jar.toURI().toURL());
            }
        }
        catch (Throwable t)
        {
            // TODO throw an error
            if (logger.isErrorEnabled())
            {
                logger.error(t);
            }
        }
    }

    public String getDomain()
    {
        return domain;
    }
}
