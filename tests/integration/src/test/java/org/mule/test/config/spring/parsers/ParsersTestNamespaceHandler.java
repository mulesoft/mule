/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import org.mule.runtime.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.runtime.config.spring.parsers.IndependentDefinitionParser;
import org.mule.test.config.spring.parsers.beans.ChildBean;
import org.mule.test.config.spring.parsers.beans.OrphanBean;
import org.mule.runtime.config.spring.parsers.collection.ChildListEntryDefinitionParser;
import org.mule.runtime.config.spring.parsers.collection.ChildMapDefinitionParser;
import org.mule.runtime.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.runtime.config.spring.parsers.collection.ChildSingletonMapDefinitionParser;
import org.mule.runtime.config.spring.parsers.delegate.InheritDefinitionParser;
import org.mule.runtime.config.spring.parsers.delegate.MapDefinitionParserMutator;
import org.mule.runtime.config.spring.parsers.delegate.SingleParentFamilyDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.NamedDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.runtime.config.spring.parsers.generic.ParentDefinitionParser;

/**
 * Registers a Bean Definition Parser for handling <code><parsers-test:...></code> elements.
 *
 */
public class ParsersTestNamespaceHandler extends AbstractMuleNamespaceHandler
{

    @Override
    public void init()
    {
        registerMuleBeanDefinitionParser("orphan", new OrphanDefinitionParser(OrphanBean.class, true)).addAlias("bar", "foo").addIgnored("ignored").addCollection("offspring");
        registerMuleBeanDefinitionParser("child", new ChildDefinitionParser("child", ChildBean.class)).addAlias("bar", "foo").addIgnored("ignored").addCollection("offspring");
        registerMuleBeanDefinitionParser("mapped-child", new MapDefinitionParserMutator("map", new ChildDefinitionParser("child", ChildBean.class))).addAlias("bar", "foo").addIgnored("ignored").addCollection("offspring");
        registerMuleBeanDefinitionParser("kid", new ChildDefinitionParser("kid", ChildBean.class)).addAlias("bar", "foo").addIgnored("ignored");
        registerMuleBeanDefinitionParser("parent", new ParentDefinitionParser()).addAlias("bar", "foo").addIgnored("ignored").addCollection("offspring");
        registerMuleBeanDefinitionParser("orphan1", new NamedDefinitionParser("orphan1")).addAlias("bar", "foo").addIgnored("ignored").addCollection("offspring");
        registerMuleBeanDefinitionParser("orphan2", new NamedDefinitionParser("orphan2")).addAlias("bar", "foo").addIgnored("ignored");
        registerMuleBeanDefinitionParser("map-entry", new ChildMapEntryDefinitionParser("map", "key", "value")).addCollection("map");
        registerMuleBeanDefinitionParser("map-entry-combiner", new ChildSingletonMapDefinitionParser("map")).addCollection("map");
        registerMuleBeanDefinitionParser("properties", new ChildMapDefinitionParser("map")).addCollection("map");
        registerBeanDefinitionParser("list-entry", new ChildListEntryDefinitionParser("list"));
        registerMuleBeanDefinitionParser("named", new NamedDefinitionParser()).addAlias("bar", "foo").addIgnored("ignored");
        registerMuleBeanDefinitionParser("inherit",
                new InheritDefinitionParser(
                        new OrphanDefinitionParser(OrphanBean.class, true),
                        new NamedDefinitionParser())).addAlias("bar", "foo").addIgnored("ignored").addCollection("offspring");

        registerBeanDefinitionParser("list-element-test-1", new ChildListEntryDefinitionParser("kids", "listAttribute"));
        registerBeanDefinitionParser("list-element-test-2",
                new SingleParentFamilyDefinitionParser(
                        new OrphanDefinitionParser(OrphanBean.class, true))
                        .addChildDelegate("kid1", new ChildListEntryDefinitionParser("kids", "kid1"))
                        .addChildDelegate("kid2", new ChildListEntryDefinitionParser("kids", "kid2")));
        // simpler list element parser doesn't support dynamic attribute
//        registerBeanDefinitionParser("list-element-test-3", new AllAttributeChildDefinitionParser(new ChildListEntryDefinitionParser("kids")));

        // TODO ComplexComponentDefinitionParser is not longer used, is there any way to rewrite/reuse the "factory" element for testing?
//         registerBeanDefinitionParser("factory",
//                new ComplexComponentDefinitionParser(
//                        new SimpleComponentDefinitionParser(ChildBean.class),
//                        (ChildDefinitionParser) new ChildDefinitionParser("child", ChildBean.class).addAlias("bar", "foo").addIgnored("ignored").addCollection("offspring")));

        registerBeanDefinitionParser("no-name", new OrphanDefinitionParser(OrphanBean.class, true));
        registerBeanDefinitionParser("no-name-2", new IndependentDefinitionParser());
        registerBeanDefinitionParser("container", new ThirdPartyContainerDefinitionParser());
    }

}
