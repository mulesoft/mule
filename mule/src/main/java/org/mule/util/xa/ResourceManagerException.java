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
 *
 */
package org.mule.util.xa;

import org.mule.config.i18n.Message;
import org.mule.umo.UMOException;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class ResourceManagerException extends UMOException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -2710661653314559260L;

    /**
     * 
     */
    public ResourceManagerException()
    {
        super();
    }

    /**
     * @param message
     */
    public ResourceManagerException(Message message)
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
    public ResourceManagerException(Message message, Throwable cause)
    {
        super(message, cause);
    }

}
