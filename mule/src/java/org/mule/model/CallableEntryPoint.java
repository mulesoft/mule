/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.model;

import org.mule.umo.model.UMOEntryPoint;
import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;

/**
 * A simple Entrypoint for the callable interface
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class CallableEntryPoint implements UMOEntryPoint
 {
    public Class[] getParameterTypes() {
        return Callable.class.getMethods()[0].getParameterTypes();
    }

    public Object invoke(Object component, UMOEventContext context) throws Exception
    {
        if(component instanceof Callable) {
            return ((Callable)component).onCall(context);
        } else {
            throw new NoSatisfiableMethodsException(component);
        }
    }

    public boolean isVoid() {
        return false;
    }

    public String getMethodName() {
        return Callable.class.getMethods()[0].getName();
    }
}
