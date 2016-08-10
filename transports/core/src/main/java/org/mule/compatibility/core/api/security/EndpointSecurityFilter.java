/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.api.security;

import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.endpoint.EndpointAware;
import org.mule.runtime.core.api.security.AuthenticationFilter;

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
