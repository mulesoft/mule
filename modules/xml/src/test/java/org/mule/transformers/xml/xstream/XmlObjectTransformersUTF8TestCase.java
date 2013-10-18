/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformers.xml.xstream;

import org.mule.api.transformer.Transformer;
import org.mule.module.xml.transformer.XmlToObject;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.util.ArrayUtils;

import java.io.UnsupportedEncodingException;

public class XmlObjectTransformersUTF8TestCase extends AbstractTransformerTestCase
{

    // this is "�b�d�f" in a Java source file encoding independent form.
    private static final String TEST_STRING = "\u00E1b\u00E7d\u00E8f";

    private final byte[] testXml;

    public XmlObjectTransformersUTF8TestCase() throws UnsupportedEncodingException
    {
        super();

        testXml = ArrayUtils.addAll("<string>".getBytes("ASCII"), ArrayUtils.addAll(
            TEST_STRING.getBytes("UTF-8"), "</string>".getBytes("ASCII")));
    }

    public Transformer getTransformer() throws Exception
    {
        return createObject(XmlToObject.class);
    }

    public Transformer getRoundTripTransformer() throws Exception
    {
        // no round tripping because ObjectToXml transforms to String, and not byte[]
        return null;
    }

    public Object getTestData()
    {
        return testXml;
    }

    public Object getResultData()
    {
        return TEST_STRING;
    }
}
