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

import org.mule.umo.UMOException;

/**
 * <code>ComponentResolverException</code> is an Exception thrown by the
 * component resolver when it fials to find a component
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class ComponentResolverException extends UMOException
{

    /**
     * @param message
     */
    public ComponentResolverException(String message)
    {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public ComponentResolverException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
