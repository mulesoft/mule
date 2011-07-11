/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer.compression;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.Transformer;
import org.mule.transformer.AbstractTransformerTestCase;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.SerializationUtils;
import org.mule.util.compression.GZipCompression;

import static org.junit.Assert.fail;

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
}
