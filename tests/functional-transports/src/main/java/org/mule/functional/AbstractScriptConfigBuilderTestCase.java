/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.compatibility.core.api.config.MuleEndpointProperties;
import org.mule.compatibility.core.api.endpoint.EndpointFactory;
import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.routing.filters.MessagePropertyFilter;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public abstract class AbstractScriptConfigBuilderTestCase extends FunctionalTestCase {

  // use legacy entry point resolver?
  private boolean legacy;

  protected AbstractScriptConfigBuilderTestCase() {
    this(false);
  }

  protected AbstractScriptConfigBuilderTestCase(boolean legacy) {
    this.legacy = legacy;
  }

  @Test
  public void testGlobalEndpointConfig() throws MuleException {
    ImmutableEndpoint endpoint = getEndpointFactory().getInboundEndpoint("fruitBowlEndpoint");
    assertNotNull(endpoint);
    assertEquals(endpoint.getEndpointURI().getAddress(), "fruitBowlPublishQ");

    MessagePropertyFilter filter = (MessagePropertyFilter) endpoint.getFilter();
    assertNotNull(filter);
    assertEquals("foo=bar", filter.getPattern());

    ImmutableEndpoint ep = getEndpointFactory().getInboundEndpoint("testEPWithCS");
    assertNotNull(ep);
  }

  /*
   * Since MULE-1933, Service no longer has properties and most properties are set on endpoint. So lets continue to test
   * properties, but on targets instead.
   */
  @Test
  public void testEndpointPropertiesConfig() throws Exception {
    ImmutableEndpoint endpoint = getEndpointFactory().getInboundEndpoint("endpointWithProps");

    Map props = endpoint.getProperties();
    assertNotNull(props);
    assertEquals("9", props.get("segments"));
    assertEquals("4.21", props.get("radius"));
    assertEquals("Juicy Baby!", props.get("brand"));

    assertNotNull(props.get("listProperties"));
    List list = (List) props.get("listProperties");
    assertEquals(3, list.size());
    assertEquals("prop1", list.get(0));
    assertEquals("prop2", list.get(1));
    assertEquals("prop3", list.get(2));

    assertNotNull(props.get("arrayProperties"));
    list = (List) props.get("arrayProperties");
    assertEquals(3, list.size());
    assertEquals("prop4", list.get(0));
    assertEquals("prop5", list.get(1));
    assertEquals("prop6", list.get(2));

    assertNotNull(props.get("mapProperties"));
    props = (Map) props.get("mapProperties");
    assertEquals("prop1", props.get("prop1"));
    assertEquals("prop2", props.get("prop2"));

    assertEquals(6, endpoint.getProperties().size());
  }

  public EndpointFactory getEndpointFactory() {
    return (EndpointFactory) muleContext.getRegistry().lookupObject(MuleEndpointProperties.OBJECT_MULE_ENDPOINT_FACTORY);
  }
}
