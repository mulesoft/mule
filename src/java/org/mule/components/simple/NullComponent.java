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
package org.mule.components.simple;

import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;

/**
 * <code>NullComponent</code> is a component that is used as a placeholder.  This
 * implementation will throw an exception if a message is received for it.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class NullComponent implements Callable
{
    public Object onCall(UMOEventContext context) throws Exception
    {
        throw new UnsupportedOperationException("This component cannot receive messages. Component is: " +
                context.getComponentDescriptor().getName());
    }
}