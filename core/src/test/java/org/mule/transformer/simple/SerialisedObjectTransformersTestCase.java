/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
