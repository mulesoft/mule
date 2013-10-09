/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.security;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.endpoint.EndpointAware;

/**
 * <code>EndpointSecurityFilter</code> is a base filter for secure filtering of
 * inbound and outbout event flow
 */
@Deprecated
public interface EndpointSecurityFilter extends AuthenticationFilter, EndpointAware
{
    void setEndpoint(ImmutableEndpoint endpoint);

    ImmutableEndpoint getEndpoint();
}
