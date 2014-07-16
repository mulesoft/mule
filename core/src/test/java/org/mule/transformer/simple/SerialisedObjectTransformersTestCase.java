/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.Transformer;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformer.AbstractTransformerTestCase;

import org.apache.commons.lang.SerializationUtils;

public class SerialisedObjectTransformersTestCase extends AbstractTransformerTestCase
{
    private Orange testObject = new Orange(new Integer(4), new Double(14.3), "nice!");

    public Transformer getTransformer() throws Exception
    {
        return createObject(SerializableToByteArray.class);
    }

    public Transformer getRoundTripTransformer() throws Exception
    {
        return createObject(ByteArrayToSerializable.class);
    }

    public Object getTestData()
    {
        return testObject;
    }

    public Object getResultData()
    {
        return SerializationUtils.serialize(testObject);
    }

}
