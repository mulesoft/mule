/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.config.spring.parsers.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.EndpointURI;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleException;

public abstract class AbstractEndpointTestCase extends FunctionalTestCase {

  public ImmutableEndpoint doTest(String name) throws MuleException {
    ImmutableEndpoint endpoint = getEndpointFactory().getInboundEndpoint(name);
    assertNotNull(endpoint);
    EndpointURI uri = endpoint.getEndpointURI();
    assertNotNull(uri);
    assertEquals("foo", uri.getAddress());
    assertEquals("test", uri.getScheme());
    return endpoint;
  }

  public EndpointFactory getEndpointFactory() {
    return (EndpointFactory) muleContext.getRegistry().lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
  }

}
