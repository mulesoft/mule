/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.transformers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import org.mule.tck.AbstractTransformerTestCase;
import org.mule.transformers.simple.ByteArrayToSerializable;
import org.mule.transformers.simple.SerializableToByteArray;
import org.mule.umo.transformer.UMOTransformer;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SerialisedObjectTransformersTestCase extends AbstractTransformerTestCase
{
    private Exception testObject = new Exception("test");

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
        try {
            ByteArrayOutputStream bs = null;
            ObjectOutputStream os = null;

            bs = new ByteArrayOutputStream();
            os = new ObjectOutputStream(bs);
            os.writeObject(testObject);
            os.flush();
            os.close();
            return bs.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    public boolean compareResults(Object src, Object result)
    {
        if (src == null && result == null)
            return true;
        if (src == null || result == null)
            return false;
        return Arrays.equals((byte[]) src, (byte[]) result);
    }

    public boolean compareRoundtripResults(Object src, Object result)
    {
        if (src == null && result == null)
            return true;
        if (src == null || result == null)
            return false;
        if (src instanceof Exception && result instanceof Exception) {
            return ((Exception) src).getMessage().equals(((Exception) result).getMessage());
        } else {
            throw new IllegalStateException("arguments are not Exceptions");
        }
    }
}
