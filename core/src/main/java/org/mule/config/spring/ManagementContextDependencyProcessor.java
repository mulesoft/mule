/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.RegistryContext;
import org.mule.impl.ManagementContextAware;
import org.mule.umo.UMOManagementContext;

/**
 * TODO
 */
public class ManagementContextDependencyProcessor implements ObjectProcessor
{
    private UMOManagementContext context;

    public ManagementContextDependencyProcessor()
    {
    }

    public ManagementContextDependencyProcessor(UMOManagementContext context)
    {
        this.context = context;
    }

    public Object process(Object object)
    {
        if(object instanceof ManagementContextAware)
        {
            if(context==null)
            {
                context= RegistryContext.getRegistry().getManagementContext();
                if(context==null)
                {
                    //todo
                    throw new NullPointerException("manContext is null");
                }
            }
            ((ManagementContextAware)object).setManagementContext(context);
        }
        return object;
    }
}
