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

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class ResourceManagerSystemException extends ResourceManagerException
{

    /**
     * Serial version
     */
    private static final long serialVersionUID = 1202058044460490597L;

    /**
     * 
     */
    public ResourceManagerSystemException()
    {
        super();
    }

    /**
     * @param message
     */
    public ResourceManagerSystemException(Message message)
    {
        super(message);
    }

    /**
     * @param cause
     */
    public ResourceManagerSystemException(Throwable cause)
    {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public ResourceManagerSystemException(Message message, Throwable cause)
    {
        super(message, cause);
    }

}
