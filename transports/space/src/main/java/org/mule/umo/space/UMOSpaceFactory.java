/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.space;

import org.mule.umo.endpoint.UMOImmutableEndpoint;

/**
 * A factory for creating Mule Space facade objects
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOSpaceFactory
{

    /**
     * Creates a space based on the endpoint URI and can use additional properties,
     * transaction info, security info and filters associated with the endpoint
     * 
     * @param endpoint the endpoint from which to construct the space
     * @return an new Space object
     * @throws UMOSpaceException
     */
    public UMOSpace create(UMOImmutableEndpoint endpoint) throws UMOSpaceException;

    /**
     * Creates a space based on a URI identifier for the space
     * 
     * @param spaceIdentifier a URI from which to construct the space
     * @return an new Space object
     * @throws UMOSpaceException
     */
    public UMOSpace create(String spaceIdentifier) throws UMOSpaceException;
}
