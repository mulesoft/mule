/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.config.spring.parsers;

import org.mule.compatibility.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.compatibility.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.compatibility.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.compatibility.config.spring.parsers.specific.endpoint.support.AddressedEndpointDefinitionParser;
import org.mule.compatibility.config.spring.parsers.specific.endpoint.support.ChildAddressDefinitionParser;
import org.mule.compatibility.config.spring.parsers.specific.endpoint.support.ChildEndpointDefinitionParser;
import org.mule.compatibility.config.spring.parsers.specific.endpoint.support.OrphanEndpointDefinitionParser;
import org.mule.compatibility.core.endpoint.EndpointURIEndpointBuilder;
import org.mule.runtime.config.spring.handlers.AbstractMuleNamespaceHandler;

/**
 * Registers a Bean Definition Parser for handling <code><parsers-test:...></code> elements.
 *
 */
public class EndpointParsersTestNamespaceHandler extends AbstractMuleNamespaceHandler {

  @Override
  public void init() {
    registerMuleBeanDefinitionParser("address", new ChildAddressDefinitionParser("test")).addAlias("address", "host");
    registerBeanDefinitionParser("orphan-endpoint", new OrphanEndpointDefinitionParser(EndpointURIEndpointBuilder.class));
    registerBeanDefinitionParser("child-endpoint", new ChildEndpointDefinitionParser(InboundEndpointFactoryBean.class));
    registerBeanDefinitionParser("unaddressed-orphan-endpoint",
                                 new OrphanEndpointDefinitionParser(EndpointURIEndpointBuilder.class));
    registerBeanDefinitionParser("addressed-orphan-endpoint",
                                 new AddressedEndpointDefinitionParser("test", AddressedEndpointDefinitionParser.PROTOCOL,
                                                                       new OrphanEndpointDefinitionParser(EndpointURIEndpointBuilder.class),
                                                                       new String[] {"path"}, new String[] {}));
    registerBeanDefinitionParser("addressed-child-endpoint",
                                 new TransportEndpointDefinitionParser("test", InboundEndpointFactoryBean.class,
                                                                       new String[] {}));

    registerMuleBeanDefinitionParser("complex-endpoint",
                                     new TransportGlobalEndpointDefinitionParser("test",
                                                                                 TransportGlobalEndpointDefinitionParser.PROTOCOL,
                                                                                 new String[] {"path"},
                                                                                 new String[] {"string", "bar"})).addAlias("bar",
                                                                                                                           "foo");
  }

}
