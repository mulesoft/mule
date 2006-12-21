/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.model;

import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.model.UMOEntryPoint;

/**
 * A simple Entrypoint for the callable interface
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class CallableEntryPoint implements UMOEntryPoint
{
    public Class[] getParameterTypes()
    {
        return Callable.class.getMethods()[0].getParameterTypes();
    }

    public Object invoke(Object component, UMOEventContext context) throws Exception
    {
        if (component instanceof Callable)
        {
            return ((Callable)component).onCall(context);
        }
        else
        {
            throw new NoSatisfiableMethodsException(component, UMOEventContext.class);
        }
    }

    public boolean isVoid()
    {
        return false;
    }

    public String getMethodName()
    {
        return Callable.class.getMethods()[0].getName();
    }
}
