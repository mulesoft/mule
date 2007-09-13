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

import org.mule.config.spring.handlers.AbstractIgnorableNamespaceHandler;
import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.delegate.InheritDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.StringAddressEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.TransportEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.UnaddressedEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.AddressedEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.ChildAddressDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.ChildEndpointDefinitionParser;
import org.mule.config.spring.parsers.specific.endpoint.support.OrphanEndpointDefinitionParser;
import org.mule.impl.endpoint.GlobalEndpoint;
import org.mule.impl.endpoint.InboundEndpoint;

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

        registerBeanDefinitionParser("string-endpoint", new StringAddressEndpointDefinitionParser(GlobalEndpoint.class));
        registerBeanDefinitionParser("unaddressed-endpoint", new UnaddressedEndpointDefinitionParser(GlobalEndpoint.class));
        registerMuleDefinitionParser("address", new ChildAddressDefinitionParser("test")).addAlias("address", "hostname");
        registerBeanDefinitionParser("addressed-endpoint", new AddressedEndpointDefinitionParser("test", new UnaddressedEndpointDefinitionParser(GlobalEndpoint.class)));
        registerBeanDefinitionParser("orphan-endpoint", new OrphanEndpointDefinitionParser(GlobalEndpoint.class));
        registerBeanDefinitionParser("child-endpoint", new ChildEndpointDefinitionParser(InboundEndpoint.class));
        registerBeanDefinitionParser("unaddressed-orphan-endpoint", new OrphanEndpointDefinitionParser(GlobalEndpoint.class));
        registerBeanDefinitionParser("addressed-orphan-endpoint", new AddressedEndpointDefinitionParser("test", new OrphanEndpointDefinitionParser(GlobalEndpoint.class)));
        registerBeanDefinitionParser("addressed-child-endpoint", new TransportEndpointDefinitionParser("test", InboundEndpoint.class));
    }

}
