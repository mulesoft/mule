/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.transformers;

import org.mule.tck.AbstractTransformerTestCase;
import org.mule.transformers.simple.ByteArrayToSerialisable;
import org.mule.transformers.simple.SerialisableToByteArray;
import org.mule.umo.transformer.UMOTransformer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class SerialisedObjectTransformersTestCase extends AbstractTransformerTestCase
{
    private Exception testObject = new Exception("test");

    public UMOTransformer getTransformer() throws Exception
    {
        return new SerialisableToByteArray();
    }

    public UMOTransformer getRoundTripTransformer() throws Exception
    {
        return new ByteArrayToSerialisable();
    }

    public Object getTestData()
    {
        return testObject;
    }

    public Object getResultData()
    {
        try
        {
            ByteArrayOutputStream bs = null;
            ObjectOutputStream os = null;

            bs = new ByteArrayOutputStream();
            os = new ObjectOutputStream(bs);
            os.writeObject(testObject);
            os.flush();
            os.close();
            return bs.toByteArray();
        } catch (IOException e)
        {
            throw new IllegalStateException(e.getMessage());
        }
    }
}
