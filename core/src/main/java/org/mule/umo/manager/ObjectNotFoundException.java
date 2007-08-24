/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.manager;

import org.mule.config.i18n.CoreMessages;

/**
 * <code>ObjectNotFoundException</code> is thrown when a reference to a component
 * in a configured container is not found
 */
public class ObjectNotFoundException extends ContainerException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 5012452325639544484L;

    /**
     * @param componentName the name of the component that could not be found
     */
    public ObjectNotFoundException(String componentName)
    {
        super(CoreMessages.objectNotFound(componentName));
    }

    /**
     * @param componentName the name of the component that could not be found
     * @param cause the exception that cause this exception to be thrown
     */
    public ObjectNotFoundException(String componentName, Throwable cause)
    {
        super(CoreMessages.objectNotFound(componentName), cause);
    }
}
