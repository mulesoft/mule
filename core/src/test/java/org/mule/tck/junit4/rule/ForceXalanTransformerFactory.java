/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.rule;

/**
 * Adds a system property to force the usage of xalan transformer factory
 */
public class ForceXalanTransformerFactory extends SystemProperty
{

    public ForceXalanTransformerFactory()
    {
        super("javax.xml.transform.TransformerFactory", "org.apache.xalan.processor.TransformerFactoryImpl");
    }

}
