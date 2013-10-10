/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

import org.mule.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ObjectByteArrayTransformersWithResultStreamTestCase extends
    ObjectByteArrayTransformersWithObjectsTestCase
{

    @Override
    public Object getResultData()
    {
        byte[] resultData = (byte[])super.getResultData();
        return new ByteArrayInputStream(resultData);
    }

    @Override
    public boolean compareResults(Object expected, Object result)
    {
        if (expected instanceof InputStream)
        {
            InputStream input = (InputStream)expected;
            byte[] bytes = IOUtils.toByteArray(input);
            return super.compareResults(bytes, result);
        }
        return super.compareResults(expected, result);
    }

}
