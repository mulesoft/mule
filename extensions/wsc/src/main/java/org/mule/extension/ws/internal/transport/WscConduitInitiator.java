/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal.transport;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.cxf.wsdl.EndpointReferenceUtils.setServiceAndPortName;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 * A simple factory for the custom {@link WscConduit} used by CXF.
 *
 * @since 4.0
 */
class WscConduitInitiator implements ConduitInitiator {

  @Override
  public Conduit getConduit(EndpointInfo targetInfo) throws IOException {
    return getConduit(targetInfo, null);
  }

  @Override
  public Conduit getConduit(EndpointInfo localInfo, EndpointReferenceType target) throws IOException {
    return new WscConduit(getTargetReference(localInfo, target));
  }

  @Override
  public Set<String> getUriPrefixes() {
    return Stream.of("http://", "https://", "jms://", "smtp://", "tcp://").collect(toSet());
  }

  @Override
  public List<String> getTransportIds() {
    return Stream.of("http://schemas.xmlsoap.org/soap/http").collect(toList());
  }

  private EndpointReferenceType getTargetReference(EndpointInfo endpointInfo, EndpointReferenceType referenceType) {
    if (referenceType != null) {
      return referenceType;
    }
    EndpointReferenceType ref = new EndpointReferenceType();
    AttributedURIType address = new AttributedURIType();
    address.setValue(endpointInfo.getAddress());
    ref.setAddress(address);
    if (endpointInfo.getService() != null) {
      setServiceAndPortName(ref, endpointInfo.getService().getName(), endpointInfo.getName().getLocalPart());
    }
    return ref;
  }
}
