/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;


public class ObjectByteArrayTransformersWithStringsTestCase extends ObjectByteArrayTransformersWithObjectsTestCase
{
    private String testObject = "test";

    public Object getTestData()
    {
        return testObject;
    }

    public Object getResultData()
    {
        return testObject.getBytes();
    }
}
