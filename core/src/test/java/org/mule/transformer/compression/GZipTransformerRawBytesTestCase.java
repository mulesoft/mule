/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.compression;

import static org.junit.Assert.fail;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.Transformer;

import java.io.UnsupportedEncodingException;

/**
 * Tests {@link GZipCompressTransformer} and its counterpart, the {@link GZipUncompressTransformer} with raw bytes as input.
 */
public class GZipTransformerRawBytesTestCase extends GZipTransformerTestCase
{

    @Override
    public Object getResultData()
    {
        try
        {
            return strat.compressByteArray((byte[]) this.getTestData());
        }
        catch (Exception e)
        {
            fail(e.getMessage());
            return null;
        }
    }

    @Override
    public Object getTestData()
    {
        try
        {
            return ((String) super.getTestData()).getBytes("UTF8");
        }
        catch (UnsupportedEncodingException uex)
        {
            fail(uex.getMessage());
            return null;
        }
    }

    @Override
    public Transformer getRoundTripTransformer()
    {
        GZipUncompressTransformer transformer = new GZipUncompressTransformer();
        transformer.setMuleContext(muleContext);

        try
        {
            transformer.initialise();
        }
        catch (InitialisationException e)
        {
            fail(e.getMessage());
        }

        return transformer;
    }

    @Override
    public void doTestBadReturnType(Transformer tran, Object src) throws Exception
    {
        /*
         * Disabled, otherwise the test for invalid return types would fail. The
         * "invalid" class that is configured as returnType by the test harness would
         * actually be a valid return type for our roundTripTransformer.
         */
    }

}
