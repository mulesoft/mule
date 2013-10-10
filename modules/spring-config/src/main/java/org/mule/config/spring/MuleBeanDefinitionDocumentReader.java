/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
    protected BeanDefinitionParserDelegate createHelper(XmlReaderContext readerContext, Element root, BeanDefinitionParserDelegate parentDelegate)
    {
        BeanDefinitionParserDelegate delegate = new MuleHierarchicalBeanDefinitionParserDelegate(readerContext, this);
        delegate.initDefaults(root, parentDelegate);
        return delegate;
    }

    /* Keep backward compatibility with spring 3.0 */
    protected BeanDefinitionParserDelegate createHelper(XmlReaderContext readerContext, Element root)
    {
        BeanDefinitionParserDelegate delegate = new MuleHierarchicalBeanDefinitionParserDelegate(readerContext, this);
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
