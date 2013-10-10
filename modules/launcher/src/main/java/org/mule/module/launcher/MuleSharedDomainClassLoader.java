/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.launcher;

import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.FileUtils;
import org.mule.util.SystemUtils;

import java.io.File;
import java.net.URL;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *  Load $MULE_HOME/lib/shared/<domain> libraries.
 */
public class MuleSharedDomainClassLoader extends GoodCitizenClassLoader
{

    protected transient Log logger = LogFactory.getLog(getClass());

    private final String domain = "undefined";

    @SuppressWarnings("unchecked")
    public MuleSharedDomainClassLoader(String domain, ClassLoader parent)
    {
        super(new URL[0], parent);
        try
        {
            File domainDir = new File(MuleContainerBootstrapUtils.getMuleHome(), "lib/shared/" + domain);
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
            throw new RuntimeException(t);
        }
    }

    public String getDomain()
    {
        return domain;
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s]@%s", getClass().getName(),
                             domain,
                             Integer.toHexString(System.identityHashCode(this)));
    }

}
