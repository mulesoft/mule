/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.umo.transformer;

import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Initialisable;

/**
 * <code>UMOTransformer</code> can be chained together to covert message payloads from one
 * object type to another.

 */
public interface UMOBaseTransformer extends Initialisable
{
    /**
     * The endpoint that this transformer is attached to
     * @return the endpoint associated with the transformer
     */
    UMOImmutableEndpoint getEndpoint();

    /**
     * Sets the endpoint associated with with this connector.
     * This should be idempotent, since endpoints do not guarantee it will only
     * be called once.
     * @param endpoint sets the endpoint associated with the transfromer
     */
    void setEndpoint(UMOImmutableEndpoint endpoint);

    /**
     * @param newName the logical name for the transformer
     */
    void setName(String newName);

    /**
     * @return the logical name of the transformer
     */
    String getName();

}
