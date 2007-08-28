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

import org.mule.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.InheritDefinitionParser;
import org.mule.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.generic.ParentDefinitionParser;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Registers a Bean Definition Parser for handling <code><parsers-test:...></code> elements.
 *
 */
public class ParsersTestNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("orphan", new OrphanDefinitionParser(OrphanBean.class, true).addAlias("bar", "foo").addIgnored("ignored").addCollection("offspring"));
        registerBeanDefinitionParser("child", new ChildDefinitionParser("child", ChildBean.class).addAlias("bar", "foo").addIgnored("ignored").addCollection("offspring"));
        registerBeanDefinitionParser("kid", new ChildDefinitionParser("kid", ChildBean.class).addAlias("bar", "foo").addIgnored("ignored"));
        registerBeanDefinitionParser("parent", new ParentDefinitionParser().addAlias("bar", "foo").addIgnored("ignored").addCollection("offspring"));
        registerBeanDefinitionParser("orphan1", new NamedDefinitionParser("orphan1").addAlias("bar", "foo").addIgnored("ignored").addCollection("offspring"));
        registerBeanDefinitionParser("orphan2", new NamedDefinitionParser("orphan2").addAlias("bar", "foo").addIgnored("ignored"));
        registerBeanDefinitionParser("map-entry", new ChildMapEntryDefinitionParser("map", "key", "value"));
        registerBeanDefinitionParser("list-entry", new ChildListEntryDefinitionParser("list"));
        registerBeanDefinitionParser("named", new NamedDefinitionParser().addAlias("bar", "foo").addIgnored("ignored"));
        registerBeanDefinitionParser("inherit", new InheritDefinitionParser(
                new OrphanDefinitionParser(OrphanBean.class, true),
                new NamedDefinitionParser()).addAlias("bar", "foo").addIgnored("ignored").addCollection("offspring"));
    }

}
