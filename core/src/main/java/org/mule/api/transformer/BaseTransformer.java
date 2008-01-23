/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.transformer;

import org.mule.api.NamedObject;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.Initialisable;

/**
 * <code>Transformer</code> can be chained together to covert message payloads from one
 * object type to another.

 */
public interface BaseTransformer extends Initialisable, NamedObject
{
    /**
     * The endpoint that this transformer is attached to
     * @return the endpoint associated with the transformer
     */
    ImmutableEndpoint getEndpoint();

    /**
     * Sets the endpoint associated with with this connector.
     * This should be idempotent, since endpoints do not guarantee it will only
     * be called once.
     * @param endpoint sets the endpoint associated with the transfromer
     */
    void setEndpoint(ImmutableEndpoint endpoint);

}
