/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.transformers.compression;

import org.apache.commons.lang.SerializationUtils;
import org.mule.tck.AbstractTransformerTestCase;
import org.mule.transformers.compression.GZipCompressTransformer;
import org.mule.transformers.compression.GZipUncompressTransformer;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.compression.GZipCompression;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GZipTransformerTestCase extends AbstractTransformerTestCase
{
    private GZipCompression strat;

    protected void doSetUp() throws Exception
    {
        strat = new GZipCompression();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.AbstractTransformerTestCase#getResultData()
     */
    public Object getResultData()
    {
        try {
            return strat.compressByteArray(SerializationUtils.serialize((Serializable)getTestData()));
        }
        catch (Exception e) {
            fail(e.getMessage());
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.AbstractTransformerTestCase#getTestData()
     */
    public Object getTestData()
    {
        return "the quick brown fox jumped over the lazy dog the quick brown fox jumped over the lazy dog the quick brown fox jumped over the lazy dog";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.AbstractTransformerTestCase#getTransformers()
     */
    public UMOTransformer getTransformer()
    {
        GZipCompressTransformer transformer = new GZipCompressTransformer();
        return transformer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.AbstractTransformerTestCase#getRoundTripTransformer()
     */
    public UMOTransformer getRoundTripTransformer()
    {
        GZipUncompressTransformer transformer = new GZipUncompressTransformer();
        transformer.setReturnClass(String.class);
        try {
            transformer.initialise();
        }
        catch (InitialisationException e) {
            fail(e.getMessage());
        }
        return transformer;
    }

    public boolean compareResults(Object src, Object result)
    {
        if (src == null && result == null) {
            return true;
        }

        if (src == null || result == null) {
            return false;
        }

        if (src instanceof byte[] && result instanceof byte[]) {
            return Arrays.equals((byte[])src, (byte[])result);
        }
        else {
            return super.compareResults(src, result);
        }
    }

    public boolean compareRoundtripResults(Object src, Object result)
    {
        if (src == null && result == null) {
            return true;
        }

        if (src == null || result == null) {
            return false;
        }

        if (src instanceof byte[] && result instanceof byte[]) {
            return Arrays.equals((byte[])src, (byte[])result);
        }
        else {
            return super.compareResults(src, result);
        }
    }

}
