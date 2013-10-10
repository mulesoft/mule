/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.codec;

import java.io.ByteArrayInputStream;

public class XmlEntityTransformersStreamingTestCase extends XMLEntityTransformersTestCase
{

    @Override
    public Object getTestData()
    {
        String string = (String)super.getTestData();
        return new ByteArrayInputStream(string.getBytes());
    }

    @Override
    public Object getResultData()
    {
        String string = (String)super.getResultData();
        return new ByteArrayInputStream(string.getBytes());
    }

}
