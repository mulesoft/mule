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
package org.mule.umo.security;

import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.UMOEncryptionStrategy;

import java.util.List;

/**
 * <code>UMOSecurityManager</code> is responsible for managing a one or more
 * security providers.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public interface UMOSecurityManager extends Initialisable
{
    public UMOAuthentication authenticate(UMOAuthentication authentication) throws UMOSecurityException;

    public void addProvider(UMOSecurityProvider provider);

    public UMOSecurityProvider getProvider(String name);

    public UMOSecurityProvider removeProvider(String name);

    public List getProviders();

    public void setProviders(List providers);

    public UMOSecurityContext createSecurityContext(UMOAuthentication authentication) throws UnknownAuthenticationTypeException;

    public UMOEncryptionStrategy getEncryptionStrategy(String name);

    public void addEncryptionStrategy(String name, UMOEncryptionStrategy strategy);

    public UMOEncryptionStrategy removeEncryptionStrategy(String name);
}
