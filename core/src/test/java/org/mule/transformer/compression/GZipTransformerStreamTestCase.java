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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Tests {@link GZipCompressTransformer} and its counterpart, the {@link GZipUncompressTransformer} with streams as inputs.
 */
public class GZipTransformerStreamTestCase extends GZipTransformerTestCase
{

    @Override
    public Object getResultData()
    {
        try
        {
            return strat.compressInputStream((InputStream) getTestData());

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
        return new ByteArrayInputStream(TEST_DATA.getBytes());
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
}


