/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.api.MuleContext;
import org.mule.config.ConfigResource;

import org.springframework.beans.BeansException;

/**
 * A specialization of {@link MuleArtifactContext} for domains
 *
 * @since 3.6.0
 */
final class MuleDomainContext extends MuleArtifactContext
{

    MuleDomainContext(MuleContext muleContext, ConfigResource[] configResources) throws BeansException
    {
        super(muleContext, configResources);
    }

    @Override
    protected Class<MuleDomainBeanDefinitionDocumentReader> getBeanDefinitionDocumentReaderClass()
    {
        return MuleDomainBeanDefinitionDocumentReader.class;
    }

}
