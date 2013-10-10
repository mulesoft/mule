/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.simple;

import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformerTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HexStringByteArrayTransformersTestCase extends AbstractTransformerTestCase
{

    public Transformer getTransformer()
    {
        return new HexStringToByteArray();
    }

    public Transformer getRoundTripTransformer()
    {
        return new ByteArrayToHexString();
    }

    public Object getTestData()
    {
        return "01020aff";
    }

    public Object getResultData()
    {
        return new byte[]{1, 2, 10, (byte)0xff};
    }

    @Override
    public boolean compareResults(Object src, Object result)
    {
        if (src == null && result == null)
        {
            return true;
        }
        if (src == null || result == null)
        {
            return false;
        }
        return Arrays.equals((byte[])src, (byte[])result);
    }

    @Override
    public boolean compareRoundtripResults(Object src, Object result)
    {
        if (src == null && result == null)
        {
            return true;
        }
        if (src == null || result == null)
        {
            return false;
        }
        return src.equals(result);
    }

    // extra test for uppercase output
    @Test
    public void testUppercase() throws TransformerException
    {
        ByteArrayToHexString t = new ByteArrayToHexString();
        t.setUpperCase(true);

        assertEquals(((String)getTestData()).toUpperCase(), t.transform(getResultData()));
    }
    
    @Test
    public void testStreaming() throws TransformerException
    {
        ByteArrayToHexString transformer = new ByteArrayToHexString();
        InputStream input = new ByteArrayInputStream((byte[]) this.getResultData());
        
        assertEquals(this.getTestData(), transformer.transform(input));
    }

}
