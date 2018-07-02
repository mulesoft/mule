/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml.xslt;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;

import net.sf.saxon.TransformerFactoryImpl;

/**
 * Created for testing reset operation on underlying controller
 *
 */
public class TestTransformerFactoryImpl extends TransformerFactoryImpl
{
    public static javax.xml.transform.Transformer TRANSFORMER;

    @Override
    public javax.xml.transform.Transformer newTransformer(Source source) throws TransformerConfigurationException
    {
        TRANSFORMER = super.newTransformer(source);
        return TRANSFORMER;
    }
}
