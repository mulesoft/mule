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

import org.mule.config.i18n.CoreMessages;
import org.mule.umo.UMOException;

/**
 * <code>EncryptionStrategyNotFoundException</code> is thrown by the
 * UMOSecurityManager when an encryption scheme is set in a property or header that
 * has not been registered witrh the manager
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class EncryptionStrategyNotFoundException extends UMOException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 3916371211189075139L;

    public EncryptionStrategyNotFoundException(String strategyName)
    {
        super(CoreMessages.authNoEncryptionStrategy(strategyName));
    }

    public EncryptionStrategyNotFoundException(String strategyName, Throwable cause)
    {
        super(CoreMessages.authNoEncryptionStrategy(strategyName), cause);
    }
}
