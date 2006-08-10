/*
 * $Id
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the BSD style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.transformers;

import org.mule.tck.AbstractTransformerTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformers.simple.ByteArrayToSerializable;
import org.mule.transformers.simple.SerializableToByteArray;
import org.mule.umo.transformer.UMOTransformer;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.SerializationUtils;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision: 1506 $
 */
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
