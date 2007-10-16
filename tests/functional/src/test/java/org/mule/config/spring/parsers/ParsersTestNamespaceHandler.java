/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers;

import org.mule.config.spring.factories.InboundEndpointFactoryBean;
import org.mule.config.spring.handlers.AbstractIgnorableNamespaceHandler;
import org.mule.config.spring.parsers.collection.AttributeListEntryDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.delegate.AllAttributeChildDefinitionParser;
import org.mule.config.spring.parsers.delegate.InheritDefinitionParser;
import org.mule.config.spring.parsers.delegate.SingleParentFamilyDefinitionParser;
import org.mule.config.spring.parsers.delegate.MapDefinitionParserMutator;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.StringAddressEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.UnaddressedEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportGlobalEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.AddressedEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.ChildAddressDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.ChildEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.OrphanEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.ComplexComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.SimpleComponentDefinitionParser;
import org.mule.config.spring.parsers.specific.SimplePojoServiceDefinitionParser;
import org.mule.config.spring.parsers.beans.ChildBean;
import org.mule.config.spring.parsers.beans.OrphanBean;
import org.mule.impl.endpoint.EndpointURIEndpointBuilder;

/**
 * Registers a Bean Definition Parser for handling <code><parsers-test:...></code> elements.
 *
 */
public class ParsersTestNamespaceHandler extends AbstractIgnorableNamespaceHandler
{

    public void init()
    {
        registerMuleDefinitionParser("orphan", new OrphanDefinitionParser(OrphanBean.class, true)).addAlias("bar", "foo").addIgnored("ignored").addCollection("offspring");
        registerMuleDefinitionParser("child", new ChildDefinitionParser("child", ChildBean.class)).addAlias("bar", "foo").addIgnored("ignored").addCollection("offspring");
        registerDelegateDefinitionParser("mapped-child", new MapDefinitionParserMutator("map", new ChildDefinitionParser("child", ChildBean.class))).addAlias("bar", "foo").addIgnored("ignored").addCollection("offspring");
        registerMuleDefinitionParser("kid", new ChildDefinitionParser("kid", ChildBean.class)).addAlias("bar", "foo").addIgnored("ignored");
        registerMuleDefinitionParser("parent", new ParentDefinitionParser()).addAlias("bar", "foo").addIgnored("ignored").addCollection("offspring");
        registerMuleDefinitionParser("orphan1", new NamedDefinitionParser("orphan1")).addAlias("bar", "foo").addIgnored("ignored").addCollection("offspring");
        registerMuleDefinitionParser("orphan2", new NamedDefinitionParser("orphan2")).addAlias("bar", "foo").addIgnored("ignored");
        registerBeanDefinitionParser("map-entry", new ChildMapEntryDefinitionParser("map", "key", "value"));
        registerBeanDefinitionParser("list-entry", new ChildListEntryDefinitionParser("list"));
        registerMuleDefinitionParser("named", new NamedDefinitionParser()).addAlias("bar", "foo").addIgnored("ignored");
        registerDelegateDefinitionParser("inherit", new InheritDefinitionParser(
                new OrphanDefinitionParser(OrphanBean.class, true),
                new NamedDefinitionParser())).addAlias("bar", "foo").addIgnored("ignored").addCollection("offspring");

        registerBeanDefinitionParser("string-endpoint", new StringAddressEndpointDefinitionParser(EndpointURIEndpointBuilder.class));
        registerBeanDefinitionParser("unaddressed-endpoint", new UnaddressedEndpointDefinitionParser(EndpointURIEndpointBuilder.class));
        registerMuleDefinitionParser("address", new ChildAddressDefinitionParser("test")).addAlias("address", "hostname");
        registerBeanDefinitionParser("addressed-endpoint", new AddressedEndpointDefinitionParser("test", new UnaddressedEndpointDefinitionParser(EndpointURIEndpointBuilder.class)));
        registerBeanDefinitionParser("orphan-endpoint", new OrphanEndpointDefinitionParser(EndpointURIEndpointBuilder.class));
        registerBeanDefinitionParser("child-endpoint", new ChildEndpointDefinitionParser(InboundEndpointFactoryBean.class));
        registerBeanDefinitionParser("unaddressed-orphan-endpoint", new OrphanEndpointDefinitionParser(EndpointURIEndpointBuilder.class));
        registerBeanDefinitionParser("addressed-orphan-endpoint", new AddressedEndpointDefinitionParser("test", new OrphanEndpointDefinitionParser(EndpointURIEndpointBuilder.class)));
        registerBeanDefinitionParser("addressed-child-endpoint", new TransportEndpointDefinitionParser("test", InboundEndpointFactoryBean.class));

        registerBeanDefinitionParser("list-element-test-1", new AttributeListEntryDefinitionParser("kids", "listAttribute"));
        registerBeanDefinitionParser("list-element-test-2",
                new SingleParentFamilyDefinitionParser(
                        new OrphanDefinitionParser(OrphanBean.class, true))
                        .addChildDelegate("kid1", new AttributeListEntryDefinitionParser("kids", "kid1"))
                        .addChildDelegate("kid2", new AttributeListEntryDefinitionParser("kids", "kid2")));
        registerBeanDefinitionParser("list-element-test-3", new AllAttributeChildDefinitionParser(new AttributeListEntryDefinitionParser("kids")));

        registerBeanDefinitionParser("factory",
                new ComplexComponentDefinitionParser(
                        new SimplePojoServiceDefinitionParser(ChildBean.class, "object"),
                        (ChildDefinitionParser) new ChildDefinitionParser("child", ChildBean.class).addAlias("bar", "foo").addIgnored("ignored").addCollection("offspring")));

        registerDelegateDefinitionParser("complex-endpoint",
                new TransportGlobalEndpointDefinitionParser("test", new String[]{"string", "bar"})).addAlias("bar", "foo");
    }

}
