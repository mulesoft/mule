/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import org.mule.module.launcher.MuleSharedDomainClassLoader;
import org.mule.module.launcher.artifact.ArtifactClassLoader;
import org.mule.util.StringUtils;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class MuleDomainClassLoaderRepository implements DomainClassLoaderRepository
{

    private Map<String, ArtifactClassLoader> domainArtifactClassLoaders = new HashMap<String, ArtifactClassLoader>();

    @Override
    public synchronized ArtifactClassLoader getDomainClassLoader(String domain)
    {
        if (domainArtifactClassLoaders.containsKey(domain))
        {
            return domainArtifactClassLoaders.get(domain);
        }
        ArtifactClassLoader classLoader = new MuleSharedDomainClassLoader(StringUtils.isBlank(domain) ? DomainFactory.DEFAULT_DOMAIN_NAME : domain, getClass().getClassLoader());
        classLoader = createClassLoaderUnregisterWrapper(classLoader);
        domainArtifactClassLoaders.put(domain, classLoader);
        return classLoader;
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
        };
    }
}
