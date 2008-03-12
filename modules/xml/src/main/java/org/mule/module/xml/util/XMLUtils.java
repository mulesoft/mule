/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.util;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.dom4j.io.DOMReader;
import org.w3c.dom.Document;

/**
 * General utility methods for working with XML.
 */
public class XMLUtils
{
    public static final String TRANSFORMER_FACTORY_JDK5 = "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl";

    /**
     * Converts a DOM to an XML string.
     */
    public static String toXml(Document dom)
    {
        return new DOMReader().read(dom).asXML();
    }

    /**
     * @return a new XSLT transformer
     * @throws TransformerConfigurationException if no TransformerFactory can be located in the
     * runtime environment.
     */
    public static Transformer getTransformer() throws TransformerConfigurationException
    {
        TransformerFactory tf;
        try
        {
            tf = TransformerFactory.newInstance();
        }
        catch (TransformerFactoryConfigurationError e)
        {
            System.setProperty("javax.xml.transform.TransformerFactory", TRANSFORMER_FACTORY_JDK5);
            tf = TransformerFactory.newInstance();
        }
        if (tf != null)
        {
            return tf.newTransformer();
        }
        else
        {
            throw new TransformerConfigurationException("Unable to instantiate a TransformerFactory");
        }
    }
}
