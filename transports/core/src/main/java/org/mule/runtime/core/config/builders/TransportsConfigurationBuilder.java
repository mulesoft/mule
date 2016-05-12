/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.config.builders;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleEndpointProperties;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.endpoint.DefaultEndpointFactory;

public class TransportsConfigurationBuilder extends DefaultsConfigurationBuilder
{

    @Override
    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        super.doConfigure(muleContext);

        MuleRegistry registry = muleContext.getRegistry();

        registry.registerObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY, new DefaultEndpointFactory());

    }
}
