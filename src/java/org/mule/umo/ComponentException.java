/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.umo;

/**
 * <code>ComponentException</code> should be thrown when some action on a component
 * fails such as starting or stopping
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class ComponentException extends UMOException
{
    /**
     * @param message the exception message
     */
    public ComponentException(String message, UMOComponent component)
    {
        super(message + ". Component is: " + component.getDescriptor().getName());
    }

    /**
     * @param message the exception message
     * @param cause   the exception that cause this exception to be thrown
     */
    public ComponentException(String message, UMOComponent component, Throwable cause)
    {
        super(message + ". Component is: " + component.getDescriptor().getName() + ". Error is: " + cause.getMessage(), cause);
    }
}
