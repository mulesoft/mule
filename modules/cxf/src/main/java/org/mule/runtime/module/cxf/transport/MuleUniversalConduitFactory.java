/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.transport;

import org.mule.runtime.module.cxf.CxfConfiguration;

import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

public interface MuleUniversalConduitFactory {

  /**
   * @param ei The Endpoint being invoked by this destination.
   * @param t The EPR associated with this Conduit - i.e. the reply destination.
   */
  MuleUniversalConduit create(MuleUniversalTransport transport, CxfConfiguration configuration, EndpointInfo ei,
                              EndpointReferenceType t);

}
