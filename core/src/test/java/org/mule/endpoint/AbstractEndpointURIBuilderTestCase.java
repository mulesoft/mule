/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.endpoint;

import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.tck.AbstractMuleTestCase;

import java.net.URI;
import java.net.URISyntaxException;

public class AbstractEndpointURIBuilderTestCase extends AbstractMuleTestCase
{
    // Test for MULE-2720
    public void testGetPropertiesForURI() throws MalformedEndpointException, URISyntaxException
    {
        UrlEndpointURIBuilder endpointURIBuilder = new UrlEndpointURIBuilder();
        endpointURIBuilder.build(new URI("ftp://test%25user:test@192.168.1.12:21"));
        assertEquals("test%user:test", endpointURIBuilder.userInfo);
    }
}
