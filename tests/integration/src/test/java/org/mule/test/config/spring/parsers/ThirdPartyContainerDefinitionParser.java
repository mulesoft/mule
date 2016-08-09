/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import org.mule.runtime.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.runtime.config.spring.parsers.AbstractMuleBeanDefinitionParser;
import org.mule.runtime.config.spring.parsers.assembly.DefaultBeanAssemblerFactory;
import org.mule.runtime.config.spring.parsers.assembly.configuration.PropertyConfiguration;
import org.mule.runtime.config.spring.parsers.assembly.configuration.SimplePropertyConfiguration;
import org.mule.test.config.spring.parsers.beans.ThirdPartyContainer;
import org.mule.runtime.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.runtime.config.spring.parsers.processors.NamedSetterChildElementIterator;

public class ThirdPartyContainerDefinitionParser extends OrphanDefinitionParser {

  public ThirdPartyContainerDefinitionParser() {
    super(ThirdPartyContainer.class, true);
    addIgnored(AbstractMuleBeanDefinitionParser.ATTRIBUTE_NAME);
    addBeanFlag(MuleHierarchicalBeanDefinitionParserDelegate.MULE_NO_RECURSE);
    PropertyConfiguration configuration = new SimplePropertyConfiguration();
    registerPostProcessor(new NamedSetterChildElementIterator("thing", new DefaultBeanAssemblerFactory(), configuration));
  }

}
