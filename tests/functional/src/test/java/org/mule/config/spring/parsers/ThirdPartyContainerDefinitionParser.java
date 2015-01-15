/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers;

import org.mule.module.springconfig.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.module.springconfig.parsers.assembly.DefaultBeanAssemblerFactory;
import org.mule.module.springconfig.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.module.springconfig.parsers.assembly.configuration.SimplePropertyConfiguration;
import org.mule.module.springconfig.parsers.beans.ThirdPartyContainer;
import org.mule.module.springconfig.parsers.generic.OrphanDefinitionParser;
import org.mule.module.springconfig.parsers.processors.NamedSetterChildElementIterator;

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
