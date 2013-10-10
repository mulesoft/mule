/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.compression;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.Transformer;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.SerializationUtils;
import org.mule.util.compression.GZipCompression;

import org.junit.Test;

/**
 * Tests {@link GZipCompressTransformer} and its counterpart, the {@link GZipUncompressTransformer}.
 */
public class GZipTransformerTestCase extends AbstractTransformerTestCase
{
    protected static final String TEST_DATA = "the quick brown fox jumped over the lazy dog the quick brown fox jumped over the lazy dog the quick brown fox jumped over the lazy dog";
    protected GZipCompression strat;

    @Override
    protected void doSetUp() throws Exception
    {
        strat = new GZipCompression();
    }

    @Override
    public Object getResultData()
    {
        try
        {
            return strat.compressByteArray(SerializationUtils.serialize(TEST_DATA));
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
        return TEST_DATA;
    }

    @Override
    public Transformer getTransformer()
    {
        return new GZipCompressTransformer();
    }

    @Override
    public Transformer getRoundTripTransformer()
    {
        GZipUncompressTransformer transformer = new GZipUncompressTransformer();
        transformer.setMuleContext(muleContext);
        transformer.setReturnDataType(DataTypeFactory.STRING);

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

    @Test
    public void testCompressAndDecompress() throws Exception
    {
        Transformer compressorTransformer = getTransformer();
        Transformer decompressorTransformer = getRoundTripTransformer();

        // Compress the test data.
        Object compressedData = compressorTransformer.transform(getTestData());
        // Decompress the test data.
        Object decompressedData = decompressorTransformer.transform(compressedData);

        assertTrue(String.format("Compress and decompress process failed. Expected '%s', but got '%s'", getTestData(), decompressedData),
                   compareResults(getTestData(), decompressedData));
    }
}
