/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.routing;

import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.endpoint.DefaultEndpointFactory;
import org.mule.tck.util.endpoint.InboundEndpointWrapper;

public class SynchronizedMuleContextStartTestCase extends AbstractSynchronizedMuleContextStartTestCase {

  @Override
  protected String getConfigFile() {
    return "synchronized-mule-context-start-config.xml";
  }

  public static class DelayedStartEndpointFactory extends DefaultEndpointFactory {

    public InboundEndpoint getInboundEndpoint(EndpointBuilder builder) throws MuleException {
      InboundEndpoint endpoint = builder.buildInboundEndpoint();

      if (endpoint.getName().equals("endpoint.vm.flow2")) {
        InboundEndpointWrapper wrappedEndpoint = new DelayedStartInboundEndpointWrapper(endpoint);
        return wrappedEndpoint;
      } else {
        return endpoint;
      }
    }
  }

  public static class DelayedStartInboundEndpointWrapper extends InboundEndpointWrapper {

    public DelayedStartInboundEndpointWrapper(InboundEndpoint delegate) {
      super(delegate);
    }

    @Override
    public void start() throws MuleException {
      super.start();
      waitMessageInProgress.release();
    }
  }
}
