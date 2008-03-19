/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.xml;

import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.module.xml.transformer.ObjectToXml;
import org.mule.module.xml.transformer.XmlToObject;
import org.mule.tck.testmodels.fruit.Apple;

import java.io.ByteArrayInputStream;

public class XmlObjectTransformersTestCase extends AbstractXmlTransformerTestCase
{
    private Apple testObject = null;

    public XmlObjectTransformersTestCase()
    {
        testObject = new Apple();
        testObject.wash();
    }

    public Transformer getTransformer() throws Exception
    {
        return new ObjectToXml();
    }

    public Transformer getRoundTripTransformer() throws Exception
    {
        return new XmlToObject();
    }

    public Object getTestData()
    {
        return testObject;
    }

    public Object getResultData()
    {
        return "<org.mule.tck.testmodels.fruit.Apple>\n" + "  <bitten>false</bitten>\n"
               + "  <washed>true</washed>\n" + "</org.mule.tck.testmodels.fruit.Apple>";
    }
    
    public void testStreaming() throws TransformerException
    {
        XmlToObject transformer = new XmlToObject();
        
        String input = (String) this.getResultData();
        assertEquals(testObject, transformer.transform(new ByteArrayInputStream(input.getBytes())));
    }
}
