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
import org.springframework.core.io.Resource;

/**
 * MuleApplicationContext is replaced by {@code MuleArtifactContext} in Mule 3.5. This class is kept for backwards
 * compatibility.
 * @deprecated Use {@code MuleArtifactContext}
 */
@Deprecated
public class MuleApplicationContext extends MuleArtifactContext
{

    public MuleApplicationContext(MuleContext muleContext, ConfigResource[] configResources) throws BeansException
    {
        super(muleContext, configResources);
    }

    public MuleApplicationContext(MuleContext muleContext, Resource[] springResources) throws BeansException
    {
        super(muleContext, springResources);
    }
}
