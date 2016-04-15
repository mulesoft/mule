/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.xpath;

import javax.xml.xpath.XPathFactory;

import net.sf.saxon.xpath.XPathFactoryImpl;

/**
 * Implementation of {@link JaxpXPathEvaluator} which uses
 * the Saxon implementation.
 *
 * @since 3.6.0
 */
public class SaxonXpathEvaluator extends JaxpXPathEvaluator
{
    /**
     * {@inheritDoc}
     * Returns instances of {@link XPathFactoryImpl}
     */
    @Override
    protected XPathFactory createXPathFactory()
    {
        return new XPathFactoryImpl();
    }
}
