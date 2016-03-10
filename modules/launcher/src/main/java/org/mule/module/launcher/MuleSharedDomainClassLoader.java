/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher;

import org.mule.module.artifact.classloader.MuleArtifactClassLoader;
import org.mule.module.artifact.classloader.ArtifactClassLoader;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.FileUtils;
import org.mule.util.SystemUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

/**
 * Load $MULE_HOME/lib/shared/<domain> libraries.
 */
public class MuleSharedDomainClassLoader extends MuleArtifactClassLoader implements ArtifactClassLoader
{

    public static final String DOMAIN_LIBRARY_FOLDER = "lib";
    public static final String OLD_DOMAIN_LIBRARY_FOLDER = DOMAIN_LIBRARY_FOLDER + File.separator + "shared";

    private File domainDir;
    private File domainLibraryFolder;

    @SuppressWarnings("unchecked")
    public MuleSharedDomainClassLoader(String domain, ClassLoader parent)
    {
        super(domain, new URL[0], parent);
        try
        {
            validateAndGetDomainFolders();

            addUrls();
        }
        catch (RuntimeException e)
        {
            throw e;
        }
        catch (Throwable t)
        {
            throw new RuntimeException(t);
        }
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s]@%s", getClass().getName(),
                             getArtifactName(),
                             Integer.toHexString(System.identityHashCode(this)));
    }

    @Override
    public URL findResource(String name)
    {
        URL resource = super.findResource(name);
        if (resource == null)
        {
            File file = new File(domainDir + File.separator + name);
            if (file.exists())
            {
                try
                {
                    resource = file.toURI().toURL();
                }
                catch (MalformedURLException e)
                {
                    logger.debug("Failure looking for resource", e);
                }
            }
        }
        return resource;
    }

    protected void validateAndGetDomainFolders() throws Exception
    {
        File oldDomainDir = new File(MuleContainerBootstrapUtils.getMuleHome(), OLD_DOMAIN_LIBRARY_FOLDER + File.separator + getArtifactName());
        if (oldDomainDir.exists())
        {
            if (!oldDomainDir.canRead())
            {
                throw new IllegalArgumentException(
                        String.format("Shared ClassLoader Domain '%s' is not accessible", getArtifactName()));
            }
            domainLibraryFolder = oldDomainDir;
            domainDir = oldDomainDir;
            return;
        }

        File newDomainDir = new File(MuleContainerBootstrapUtils.getMuleDomainsDir() + File.separator + getArtifactName());
        if (!newDomainDir.exists())
        {
            throw new IllegalArgumentException(
                    String.format("Domain '%s' is not accessible", getArtifactName()));
        }

        if (!newDomainDir.canRead())
        {
            throw new IllegalArgumentException(
                    String.format("Domain '%s' is not accessible", getArtifactName()));
        }
        domainDir = newDomainDir;
        domainLibraryFolder = new File(newDomainDir, DOMAIN_LIBRARY_FOLDER);
        logger.info(String.format("Using domain dir %s for domain %s", domainDir.getAbsolutePath(), getArtifactName()));
    }

    protected void addUrls() throws MalformedURLException
    {
        addURL(domainLibraryFolder.getParentFile().toURI().toURL());

        if (domainLibraryFolder.exists())
        {
            Collection<File> jars = FileUtils.listFiles(domainLibraryFolder, new String[] {"jar"}, false);

            if (logger.isDebugEnabled())
            {
                StringBuilder sb = new StringBuilder();
                sb.append("Loading Shared ClassLoader Domain: ").append(getArtifactName()).append(SystemUtils.LINE_SEPARATOR);
                sb.append("=============================").append(SystemUtils.LINE_SEPARATOR);

                for (File jar : jars)
                {
                    sb.append(jar.toURI().toURL()).append(SystemUtils.LINE_SEPARATOR);
                }

                sb.append("=============================").append(SystemUtils.LINE_SEPARATOR);

                logger.debug(sb.toString());
            }

            for (File jar : jars)
            {
                addURL(jar.toURI().toURL());
            }
        }
    }

    @Override
    protected String[] getLocalResourceLocations()
    {
        return new String[] {domainDir.getAbsolutePath(), MuleContainerBootstrapUtils.getMuleConfDir().getAbsolutePath()};
    }
}
