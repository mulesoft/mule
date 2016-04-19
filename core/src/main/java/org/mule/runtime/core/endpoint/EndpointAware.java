/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.endpoint;

import org.mule.runtime.core.api.endpoint.ImmutableEndpoint;

/**
 * Interface used by objects that allow an endpoint instance to be set.
 */
public interface EndpointAware
{
    void setEndpoint(ImmutableEndpoint ep);
}
