/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.endpoints;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.compatibility.core.registry.MuleRegistryTransportHelper.lookupEndpointBuilder;

import org.mule.compatibility.core.api.endpoint.EndpointBuilder;
import org.mule.compatibility.core.api.endpoint.InboundEndpoint;
import org.mule.compatibility.core.api.endpoint.OutboundEndpoint;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.construct.Flow;

import java.nio.charset.Charset;

import org.junit.Test;

/** Test configuration of content-type in various endpoints */
public class EndpointContentTypeTestCase extends FunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "content-type-setting-endpoint-configs-flow.xml";
  }

  @Test
  public void testContentType() throws Exception {
    Flow flowService = muleContext.getRegistry().lookupObject("service");
    InboundEndpoint inbound = (InboundEndpoint) flowService.getMessageSource();
    assertThat(inbound.getMimeType().getPrimaryType(), is("text"));
    assertThat(inbound.getMimeType().getSubType(), is("xml"));
    assertThat(inbound.getEncoding(), is(UTF_8));
    OutboundEndpoint outbound = (OutboundEndpoint) flowService.getMessageProcessors().get(0);
    assertThat(outbound.getMimeType().getPrimaryType(), is("application"));
    assertThat(outbound.getMimeType().getSubType(), is("json"));
    assertThat(outbound.getEncoding(), is(Charset.forName("ISO-8859-2")));
    EndpointBuilder global = lookupEndpointBuilder(muleContext.getRegistry(), "global");
    InboundEndpoint created = global.buildInboundEndpoint();
    assertThat(created.getMimeType().getPrimaryType(), is("application"));
    assertThat(created.getMimeType().getSubType(), is("xml"));
    assertThat(created.getEncoding(), is(ISO_8859_1));
  }
}
