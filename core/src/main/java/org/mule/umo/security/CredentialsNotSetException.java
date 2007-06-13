/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.security;

import org.mule.config.i18n.Message;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * <code>CredentialsNotSetException</code> is thrown when user credentials cannot
 * be obtained from the current message
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class CredentialsNotSetException extends UnauthorisedException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -6271648179641734579L;

    public CredentialsNotSetException(Message message, UMOMessage umoMessage)
    {
        super(message, umoMessage);
    }

    public CredentialsNotSetException(Message message, UMOMessage umoMessage, Throwable cause)
    {
        super(message, umoMessage, cause);
    }

    public CredentialsNotSetException(UMOMessage umoMessage,
                                      UMOSecurityContext context,
                                      UMOImmutableEndpoint endpoint,
                                      UMOEndpointSecurityFilter filter)
    {
        super(umoMessage, context, endpoint, filter);
    }
}
