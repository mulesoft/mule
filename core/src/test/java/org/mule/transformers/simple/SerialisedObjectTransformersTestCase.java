/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.simple;

import org.mule.tck.AbstractTransformerTestCase;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.umo.transformer.UMOTransformer;

import org.apache.commons.lang.SerializationUtils;

public class SerialisedObjectTransformersTestCase extends AbstractTransformerTestCase
{
    private Orange testObject = new Orange(new Integer(4), new Double(14.3), "nice!");

    public UMOTransformer getTransformer() throws Exception
    {
        return new SerializableToByteArray();
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        return new ByteArrayToSerializable();
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
