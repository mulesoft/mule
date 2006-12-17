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

/**
 * <code>UMOTransformer</code> extends a UMOSimpleTransformer with the provision of
 * a Transformer component, which simply keeps a copy of the 'live' transfromed data,
 * so that any further transformations will use this cached data. When a component is
 * rolled back a cached copy of the source data will be returned. When the component
 * is committed the caches are cleared. Starting a component will simply put the
 * transfromer in component mode.
 */
public interface UMOTransformer extends UMOSimpleTransformer
{
    /**
     * @return the endpoint associated with the transformer
     */
    UMOImmutableEndpoint getEndpoint();

    /**
     * @param endpoint sets the endpoint associated with the transfromer
     */
    void setEndpoint(UMOImmutableEndpoint endpoint);

    boolean isSourceTypeSupported(Class aClass);

    boolean isAcceptNull();

}
