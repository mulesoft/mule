/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.compression;

import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.transformer.UMOTransformer;

import java.io.UnsupportedEncodingException;

public class GZipTransformerRawBytesTestCase extends GZipTransformerTestCase
{

    // @Override
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

    // @Override
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

    // @Override
    public UMOTransformer getRoundTripTransformer()
    {
        GZipUncompressTransformer transformer = new GZipUncompressTransformer();

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

    // @Override
    public void doTestBadReturnType(UMOTransformer tran, Object src) throws Exception
    {
        /*
         * Disabled, otherwise the test for invalid return types would fail. The
         * "invalid" class that is configured as returnType by the test harness would
         * actually be a valid return type for our roundTripTransformer.
         */
    }

}
