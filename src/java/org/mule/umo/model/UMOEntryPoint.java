/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.umo.model;

import org.mule.umo.UMOEventContext;
import org.mule.umo.transformer.TransformerException;

import java.lang.reflect.InvocationTargetException;

/**
 * <code>UMOEntryPoint</code> defines the current entry method on a component
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOEntryPoint
{
    Class[] getParameterTypes();

    Object invoke(Object component, UMOEventContext context) throws Exception;

    boolean isVoid();

    String getMethodName();
}
