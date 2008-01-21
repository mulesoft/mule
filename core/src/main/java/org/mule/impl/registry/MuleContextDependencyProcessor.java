/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.registry;

import org.mule.api.MuleContext;
import org.mule.impl.MuleContextAware;

/**
 * TODO
 * @deprecated This class is not being used.
 */
public class MuleContextDependencyProcessor implements ObjectProcessor
{
    private MuleContext context;

    public MuleContextDependencyProcessor()
    {
    }

    public MuleContextDependencyProcessor(MuleContext context)
    {
        this.context = context;
    }

    public Object process(Object object)
    {
        if(object instanceof MuleContextAware)
        {
            if(context==null)
            {
               // context= MuleServer.getMuleContext();
                if(context==null)
                {
                    return object;
                }
            }
            ((MuleContextAware)object).setMuleContext(context);
        }
        return object;
    }
}
