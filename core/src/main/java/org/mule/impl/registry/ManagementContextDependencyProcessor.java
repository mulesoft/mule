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

import org.mule.impl.ManagementContextAware;
import org.mule.umo.UMOManagementContext;

/**
 * TODO
 * @deprecated This class is not being used.
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
               // context= MuleServer.getManagementContext();
                if(context==null)
                {
                    return object;
                }
            }
            ((ManagementContextAware)object).setManagementContext(context);
        }
        return object;
    }
}
