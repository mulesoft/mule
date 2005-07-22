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
 *
 */
package org.mule.jbi.util.xa;

import javax.jbi.JBIException;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class ResourceManagerException extends JBIException
{

    /**
     * @param message
     */
    public ResourceManagerException(String message)
    {
        super(message);
    }

    /**
     * @param cause
     */
    public ResourceManagerException(Throwable cause)
    {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public ResourceManagerException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
