/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
 * for a more compact single-element approach.
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
