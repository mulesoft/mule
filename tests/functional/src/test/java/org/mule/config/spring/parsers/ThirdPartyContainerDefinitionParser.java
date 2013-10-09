/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers;

import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.config.spring.parsers.assembly.DefaultBeanAssemblerFactory;
import org.mule.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.config.spring.parsers.assembly.configuration.SimplePropertyConfiguration;
import org.mule.config.spring.parsers.beans.ThirdPartyContainer;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.processors.NamedSetterChildElementIterator;

public class ThirdPartyContainerDefinitionParser extends OrphanDefinitionParser
{

    public ThirdPartyContainerDefinitionParser()
    {
        super(ThirdPartyContainer.class, true);
        addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
        addBeanFlag(MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_RECURSE);
        PropertyConfiguration configuration = new SimplePropertyConfiguration();
        registerPostProcessor(
                new NamedSetterChildElementIterator(
                        "thing", new DefaultBeanAssemblerFactory(), configuration));
    }

}
