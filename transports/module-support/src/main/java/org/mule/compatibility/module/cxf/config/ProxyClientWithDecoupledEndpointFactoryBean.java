/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.config;

import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.module.cxf.CxfOutboundMessageProcessor;
import org.mule.runtime.module.cxf.builder.ProxyClientMessageProcessorBuilder;

import org.springframework.beans.factory.FactoryBean;

public class ProxyClientWithDecoupledEndpointFactoryBean extends ProxyClientMessageProcessorBuilder implements FactoryBean {

  @Override
  public CxfOutboundMessageProcessor build() throws MuleException {
    final CxfOutboundMessageProcessor processor = super.build();

    DecoupledEndpointBuilder.build(muleContext, decoupledEndpoint, processor, getBus());

    return processor;
  }

  @Override
  public Object getObject() throws Exception {
    return build();
  }

  @Override
  public Class<?> getObjectType() {
    return CxfOutboundMessageProcessor.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

}
