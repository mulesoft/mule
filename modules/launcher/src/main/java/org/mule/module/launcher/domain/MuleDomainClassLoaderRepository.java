/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.mule.module.artifact.classloader.ClassLoaderLookupStrategy.PARENT_FIRST;
import static org.mule.module.launcher.MuleFoldersUtil.getDomainLibFolder;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.artifact.classloader.ArtifactClassLoader;
import org.mule.module.artifact.classloader.ClassLoaderLookupStrategy;
import org.mule.module.artifact.classloader.ClassLoaderLookupPolicy;
import org.mule.module.artifact.classloader.ShutdownListener;
import org.mule.module.launcher.DeploymentException;
import org.mule.module.launcher.MuleFoldersUtil;
import org.mule.module.launcher.MuleSharedDomainClassLoader;
import org.mule.module.launcher.application.FilePackageDiscoverer;
import org.mule.module.launcher.application.PackageDiscoverer;
import org.mule.module.launcher.descriptor.DomainDescriptor;
import org.mule.module.launcher.descriptor.EmptyDomainDescriptor;
import org.mule.module.reboot.MuleContainerBootstrapUtils;
import org.mule.util.Preconditions;
import org.mule.util.SystemUtils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MuleDomainClassLoaderRepository implements DomainClassLoaderRepository
{
    protected static final Log logger = LogFactory.getLog(MuleDomainClassLoaderRepository.class);

    private final ClassLoaderLookupPolicy containerLookupPolicy;
    private Map<String, ArtifactClassLoader> domainArtifactClassLoaders = new HashMap<>();
    private ArtifactClassLoader defaultDomainArtifactClassLoader;
    private PackageDiscoverer packageDiscoverer = new FilePackageDiscoverer();

    public MuleDomainClassLoaderRepository(ClassLoaderLookupPolicy containerLookupPolicy)
    {
        this.containerLookupPolicy = containerLookupPolicy;
    }

    public void setPackageDiscoverer(PackageDiscoverer packageDiscoverer)
    {
        this.packageDiscoverer = packageDiscoverer;
    }

    @Override
    public synchronized ArtifactClassLoader getDomainClassLoader(DomainDescriptor descriptor)
    {
        String domain = descriptor.getName();
        Preconditions.checkArgument(domain != null, "Domain name cannot be null");
        if (domain.equals(DomainFactory.DEFAULT_DOMAIN_NAME))
        {
            return getDefaultDomainClassLoader();
        }
        if (domainArtifactClassLoaders.containsKey(domain))
        {
            return domainArtifactClassLoaders.get(domain);
        }
        validateDomain(domain);
        final List<URL> urls = getDomainUrls(domain);
        final Map<String, ClassLoaderLookupStrategy> domainLookStrategies = getLookStrategiesFrom(urls);
        final ClassLoaderLookupPolicy domainLookupPolicy = containerLookupPolicy.extend(domainLookStrategies);

        ArtifactClassLoader classLoader = new MuleSharedDomainClassLoader(domain, getClass().getClassLoader(), domainLookupPolicy, urls);
        classLoader = createClassLoaderUnregisterWrapper(classLoader);
        domainArtifactClassLoaders.put(domain, classLoader);
        return classLoader;
    }

    private Map<String, ClassLoaderLookupStrategy> getLookStrategiesFrom(List<URL> libraries)
    {
        final Map<String, ClassLoaderLookupStrategy> result = new HashMap<>();

        for (URL library : libraries)
        {
            Set<String> packages = packageDiscoverer.findPackages(library);
            for (String packageName : packages)
            {
                result.put(packageName, PARENT_FIRST);
            }
        }

        return result;
    }

    private List<URL> getDomainUrls(String domain) throws DeploymentException
    {
        try
        {
            List<URL> urls = new LinkedList<>();
            urls.add(MuleFoldersUtil.getDomainFolder(domain).toURI().toURL());
            File domainLibraryFolder = getDomainLibFolder(domain);

            if (domainLibraryFolder.exists())
            {
                Collection<File> jars = listFiles(domainLibraryFolder, new String[] {"jar"}, false);

                if (logger.isDebugEnabled())
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

                for (File jar : jars)
                {
                    urls.add(jar.toURI().toURL());
                }
            }

            return urls;
        }
        catch (MalformedURLException e)
        {
            throw new DeploymentException(CoreMessages.createStaticMessage(format("Cannot read domain '%s' libraries", domain)), e);
        }
    }

    @Override
    public synchronized ArtifactClassLoader getDomainClassLoader(String domain)
    {
        return getDomainClassLoader(new EmptyDomainDescriptor(domain));
    }

    @Override
    public ArtifactClassLoader getDefaultDomainClassLoader()
    {
        if (defaultDomainArtifactClassLoader != null)
        {
            return defaultDomainArtifactClassLoader;
        }
        ArtifactClassLoader classLoader = new MuleSharedDomainClassLoader(DomainFactory.DEFAULT_DOMAIN_NAME, getClass().getClassLoader(), containerLookupPolicy.extend(emptyMap()), emptyList());
        defaultDomainArtifactClassLoader = createClassLoaderUnregisterWrapper(classLoader);
        return defaultDomainArtifactClassLoader;
    }

    private void validateDomain(String domain)
    {
        File domainFolder = new File(MuleContainerBootstrapUtils.getMuleDomainsDir(), domain);
        if (!(domainFolder.exists() && domainFolder.isDirectory()) )
        {
            throw new DeploymentException(CoreMessages.createStaticMessage(format("Domain %s does not exists", domain)));
        }
    }

    private ArtifactClassLoader createClassLoaderUnregisterWrapper(final ArtifactClassLoader classLoader)
    {
        return new ArtifactClassLoader()
        {
            @Override
            public String getArtifactName()
            {
                return classLoader.getArtifactName();
            }

            @Override
            public URL findResource(String resource)
            {
                return classLoader.findResource(resource);
            }

            @Override
            public URL findLocalResource(String resource)
            {
                return classLoader.findLocalResource(resource);
            }

            @Override
            public ClassLoader getClassLoader()
            {
                return classLoader.getClassLoader();
            }

            @Override
            public void dispose()
            {
                domainArtifactClassLoaders.remove(classLoader.getArtifactName());
                classLoader.dispose();
            }

            @Override
            public void addShutdownListener(ShutdownListener listener)
            {
                classLoader.addShutdownListener(listener);
            }

            @Override
            public ClassLoaderLookupPolicy getClassLoaderLookupPolicy()
            {
                return classLoader.getClassLoaderLookupPolicy();
            }
        };
    }
}
