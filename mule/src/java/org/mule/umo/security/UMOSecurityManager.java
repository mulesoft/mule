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

import java.util.List;

import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.lifecycle.Initialisable;

/**
 * <code>UMOSecurityManager</code> is responsible for managing a one or more
 * security providers.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public interface UMOSecurityManager extends Initialisable
{
    UMOAuthentication authenticate(UMOAuthentication authentication) throws SecurityException,
            SecurityProviderNotFoundException;

    void addProvider(UMOSecurityProvider provider);

    UMOSecurityProvider getProvider(String name);

    UMOSecurityProvider removeProvider(String name);

    List getProviders();

    void setProviders(List providers);

    UMOSecurityContext createSecurityContext(UMOAuthentication authentication)
            throws UnknownAuthenticationTypeException;

    UMOEncryptionStrategy getEncryptionStrategy(String name);

    void addEncryptionStrategy(String name, UMOEncryptionStrategy strategy);

    UMOEncryptionStrategy removeEncryptionStrategy(String name);
}
