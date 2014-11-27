/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.config.spring.parsers.specific.DomainElementsValidator;

import org.springframework.beans.factory.xml.XmlReaderContext;

/**
 * Allows us to hook in our own Hierarchical Parser delegate. this enables the
 * parsing of custom spring bean elements nested within each other
 *
 * @since 3.6.0
 */
public class MuleDomainBeanDefinitionDocumentReader extends MuleBeanDefinitionDocumentReader
{

    @Override
    protected MuleHierarchicalBeanDefinitionParserDelegate createBeanDefinitionParserDelegate(XmlReaderContext readerContext)
    {
        return new MuleHierarchicalBeanDefinitionParserDelegate(readerContext, this, new DomainElementsValidator());
    }
}
