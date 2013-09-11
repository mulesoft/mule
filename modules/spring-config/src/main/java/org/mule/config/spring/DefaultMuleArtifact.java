/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.common.Capability;
import org.mule.common.MuleArtifact;

public class DefaultMuleArtifact implements MuleArtifact
{
    private Object object;

    public DefaultMuleArtifact(Object object)
    {
        this.object = object;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Capability> T getCapability(Class<T> arg0)
    {
        if (hasCapability(arg0))
        {
            return (T) object;
        }
        else
        {
            return null;
        }
    }

    @Override
    public <T extends Capability> boolean hasCapability(Class<T> arg0)
    {
        return arg0.isAssignableFrom(object.getClass());
    }

}
