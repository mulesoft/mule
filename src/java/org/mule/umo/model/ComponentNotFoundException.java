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
 *
 */
package org.mule.umo.model;



/**
 * <code>ComponentNotFoundException</code> is thrown when a reference to
 * a component in a configured container is not found
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class ComponentNotFoundException extends ComponentResolverException
{
    /**
     * @param message
     */
    public ComponentNotFoundException(String message)
    {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public ComponentNotFoundException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
