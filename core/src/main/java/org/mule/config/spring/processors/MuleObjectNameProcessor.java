/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.processors;

import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.manager.UMOContainerContext;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.MuleObjectHelper;
import org.mule.util.ObjectNameHelper;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * <code>MuleObjectNameProcessor</code> is used to set spring ids to Mule object
 * names so the the bean id and name property on the object don't both have to be
 * set.
 */

public class MuleObjectNameProcessor implements BeanPostProcessor
{
    private boolean overwrite = false;

    public Object postProcessBeforeInitialization(Object o, String s) throws BeansException
    {
        if (MuleObjectHelper.class.getName().equals(s))
        {
            return o;
        }
        
        if (o instanceof UMOConnector)
        {
            if (((UMOConnector)o).getName() == null || overwrite)
            {
                ((UMOConnector)o).setName(s);
            }
        }
        else if (o instanceof UMOTransformer)
        {
            ((UMOTransformer)o).setName(s);
        }
        else if (o instanceof UMOEndpoint)
        {
            // spring uses the class name of the object as the name if no other
            // id is set; this is no good for endpoints
            if ((((UMOEndpoint)o).getName() == null || overwrite)
                && !MuleEndpoint.class.getName().equals(s))
            {
                final UMOEndpoint endpoint = (UMOEndpoint) o;
                final String name = ObjectNameHelper.getEndpointName(endpoint);
                endpoint.setName(name);
            }
        }
        else if (o instanceof UMOComponent)
        {
            if (((UMOComponent)o).getName() == null || overwrite)
            {
                ((UMOComponent)o).setName(s);
            }
        }
        else if (o instanceof UMOModel)
        {
            if (((UMOModel)o).getName() == null || overwrite)
            {
                ((UMOModel)o).setName(s);
            }
        }
        else if (o instanceof UMOAgent)
        {
            ((UMOAgent)o).setName(s);
        }
        else if (o instanceof UMOContainerContext)
        {
            ((UMOContainerContext)o).setName(s);
        }
        return o;
    }

    public Object postProcessAfterInitialization(Object o, String s) throws BeansException
    {
        return o;
    }

    public boolean isOverwrite()
    {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite)
    {
        this.overwrite = overwrite;
    }

}
