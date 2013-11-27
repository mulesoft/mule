/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import org.mule.module.launcher.DefaultMuleSharedDomainClassLoader;
import org.mule.module.launcher.MuleSharedDomainClassLoader;
import org.mule.module.launcher.artifact.ArtifactClassLoader;
import org.mule.util.StringUtils;

public class MuleDomainClassLoaderFactory implements DomainClassLoaderFactory
{

    @Override
    public ArtifactClassLoader create(String domain)
    {
        ArtifactClassLoader classLoader;
        if (StringUtils.isBlank(domain) || DomainFactory.DEFAULT_DOMAIN_NAME.equals(domain))
        {
            classLoader = new DefaultMuleSharedDomainClassLoader(getClass().getClassLoader());
        }
        else
        {
            classLoader = new MuleSharedDomainClassLoader(domain, getClass().getClassLoader());
        }
        return classLoader;
    }
}
