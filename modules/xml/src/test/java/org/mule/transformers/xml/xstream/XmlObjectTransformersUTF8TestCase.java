/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
