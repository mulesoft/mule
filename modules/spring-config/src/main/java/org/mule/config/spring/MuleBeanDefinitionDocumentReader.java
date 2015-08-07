/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Allows us to hook in our own Hierarchical Parser delegate. this enables the
 * parsing of custom spring bean elements nested within each other
 */
public class MuleBeanDefinitionDocumentReader extends DefaultBeanDefinitionDocumentReader
{

    @Override
    protected BeanDefinitionParserDelegate createDelegate(XmlReaderContext readerContext, Element root, BeanDefinitionParserDelegate parentDelegate)
    {
        BeanDefinitionParserDelegate delegate = createBeanDefinitionParserDelegate(readerContext);
        delegate.initDefaults(root, parentDelegate);
        return delegate;
    }

    protected MuleHierarchicalBeanDefinitionParserDelegate createBeanDefinitionParserDelegate(XmlReaderContext readerContext)
    {
        return new MuleHierarchicalBeanDefinitionParserDelegate(readerContext, this);
    }

    /* Keep backward compatibility with spring 3.0 */
    protected BeanDefinitionParserDelegate createHelper(XmlReaderContext readerContext, Element root)
    {
        BeanDefinitionParserDelegate delegate = createBeanDefinitionParserDelegate(readerContext);
        delegate.initDefaults(root);
        return delegate;
    }

    /**
     * Override to reject configuration files with no namespace, e.g. mule legacy
     * configuration file.
     */
    @Override
    protected void parseBeanDefinitions(Element root, BeanDefinitionParserDelegate delegate)
    {
        if (!StringUtils.hasLength(root.getNamespaceURI()))
        {
            getReaderContext().error("Unable to locate NamespaceHandler for namespace [null]", root);
        }
        else
        {
            super.parseBeanDefinitions(root, delegate);
        }
    }

}
