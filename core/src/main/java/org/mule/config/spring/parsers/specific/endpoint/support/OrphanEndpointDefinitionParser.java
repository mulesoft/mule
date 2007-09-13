/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific.endpoint.support;

import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;

/**
 * A parser for "orphan" (top-level) endpoints - ie GlobalEndpoints.
 * Because we have automatic String -> MuleEnpointURI conversion via property editors
 * this can be used in a variety of ways.  It should work directly with a simple String
 * address attribute or, in combination with a child element (handled by
 * {@link ChildAddressDefinitionParser},
 * or embedded in
 * {@link AddressedEndpointDefinitionParser}
 * for a more compact single-eleent approach.
 *
 * <p>This class does not support references to other endpoints.</p>
 */
public class OrphanEndpointDefinitionParser extends OrphanDefinitionParser
{

    public OrphanEndpointDefinitionParser(Class endpoint)
    {
        super(endpoint, false);
        EndpointUtils.addProperties(this);
        EndpointUtils.addPostProcess(this);
    }

}
