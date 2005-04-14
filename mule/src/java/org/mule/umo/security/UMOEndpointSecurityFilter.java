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

import org.mule.umo.UMOEvent;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Initialisable;

/**
 * <code>UMOEndpointSecurityFilter</code> is a base filter for secure filtering of inbound
 * and outbout event flow
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOEndpointSecurityFilter extends Initialisable
{
    /**
     *
     * @param manager
     */
    public void setSecurityManager(UMOSecurityManager manager);

    public UMOSecurityManager getSecurityManager();

    public String getSecurityProviders();

    public void setSecurityProviders(String providers);

    public void setEndpoint(UMOImmutableEndpoint endpoint);

    public UMOImmutableEndpoint getEndpoint();

    public void setCredentialsAccessor(UMOCredentialsAccessor accessor);

    public UMOCredentialsAccessor getCredentialsAccessor();

    public void authenticate(UMOEvent event) throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException, SecurityProviderNotFoundException;
}
