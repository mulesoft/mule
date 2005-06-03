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
package org.mule.umo.security;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOException;

/**
 * <code>SecurityProviderNotFoundException</code> is thrown by the
 * UMOSecurityManager when an authentication request is made but no suitable
 * security provider can be found to process the authentication
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SecurityProviderNotFoundException extends UMOException
{
    public SecurityProviderNotFoundException(String providerName)
    {
        super(new Message(Messages.AUTH_NO_SECURITY_PROVIDER_X, providerName));
    }

    public SecurityProviderNotFoundException(String providerName, Throwable cause)
    {
        super(new Message(Messages.AUTH_NO_SECURITY_PROVIDER_X, providerName), cause);
    }
}
