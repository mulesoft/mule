/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.DefaultBeanDefinitionDocumentReader;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.w3c.dom.Element;

/**
 * Allows us to hook in our own Hierarchical Parser delegate. this enables the parsing of custom spring bean
 * elements nested within each other
 */
public class MuleBeanDefinitionDocumentReader extends DefaultBeanDefinitionDocumentReader
{

    protected BeanDefinitionParserDelegate createHelper(XmlReaderContext readerContext, Element root)
    {
        BeanDefinitionParserDelegate delegate = new MuleHierarchicalBeanDefinitionParserDelegate(readerContext);
		delegate.initDefaults(root);
		return delegate;
    }
}
