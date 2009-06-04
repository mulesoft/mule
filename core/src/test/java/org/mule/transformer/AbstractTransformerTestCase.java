/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer;

import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.testmodels.fruit.InvalidSatsuma;
import org.mule.util.IOUtils;

import java.io.InputStream;
import java.util.Arrays;

public abstract class AbstractTransformerTestCase extends AbstractMuleTestCase
{

    @Override
    protected void doSetUp() throws Exception
    {
        // setup a dummy context for transformers that are event aware
        RequestContext.setEvent(getTestEvent("test"));
    }

    @Override
    protected void doTearDown() throws Exception
    {
        RequestContext.setEvent(null);
    }

    // Remove tabs and line breaks in the passed String; this makes comparison of XML
    // fragments easier
    protected String normalizeString(String rawString)
    {
        rawString = rawString.replaceAll("\r", "");
        rawString = rawString.replaceAll("\n", "");
        return rawString.replaceAll("\t", "");
    }

    public void testTransform() throws Exception
    {
        Object result = this.getTransformer().transform(getTestData());
        assertNotNull(result);

        Object expectedResult = this.getResultData();
        assertNotNull(expectedResult);

        assertTrue(this.compareResults(expectedResult, result));
    }

    public void testRoundtripTransform() throws Exception
    {
        Transformer roundTripTransformer = this.getRoundTripTransformer();
        if (roundTripTransformer != null)
        {
            Object result = roundTripTransformer.transform(this.getResultData());
            assertNotNull(result);

            assertTrue(this.compareRoundtripResults(this.getTestData(), result));
        }
    }

    public void testBadReturnType() throws Exception
    {
        this.doTestBadReturnType(this.getTransformer(), this.getTestData());
    }

    public void testRoundtripBadReturnType() throws Exception
    {
        if (this.getRoundTripTransformer() != null)
        {
            this.doTestBadReturnType(this.getRoundTripTransformer(), this.getResultData());
        }
    }

    public void testRoundTrip() throws Exception
    {
        if (this.getRoundTripTransformer() != null)
        {
            Transformer trans = this.getTransformer();
            Transformer trans2 = this.getRoundTripTransformer();
            MuleMessage message = new DefaultMuleMessage(getTestData());
            message.applyTransformers(Arrays.asList( new Transformer[]{trans, trans2}));
            Object result = message.getPayload();
            this.compareRoundtripResults(this.getTestData(), result);
        }
    }

    public void doTestBadReturnType(Transformer tran, Object src) throws Exception
    {
        tran.setReturnClass(InvalidSatsuma.class);
        try
        {
            tran.transform(src);
            fail("Should throw exception for bad return type");
        }
        catch (TransformerException e)
        {
            // expected
        }
    }

    protected void doTestClone(Transformer original, Transformer clone) throws Exception
    {
        assertNotSame(original, clone);
    }

    public abstract Transformer getTransformer() throws Exception;

    public abstract Transformer getRoundTripTransformer() throws Exception;

    public abstract Object getTestData();

    public abstract Object getResultData();

    public boolean compareResults(Object expected, Object result)
    {
        if (expected == null && result == null)
        {
            return true;
        }

        if (expected == null || result == null)
        {
            return false;
        }

        if (expected instanceof Object[] && result instanceof Object[])
        {
            return Arrays.equals((Object[]) expected, (Object[]) result);
        }
        else if (expected instanceof byte[] && result instanceof byte[])
        {
            return Arrays.equals((byte[]) expected, (byte[]) result);
        }

        if (expected instanceof InputStream)
        {
            expected = IOUtils.toString((InputStream)expected);
        }
        
        // Special case for Strings: normalize comparison arguments
        if (expected instanceof String && result instanceof String)
        {
            expected = this.normalizeString((String) expected);
            result = this.normalizeString((String) result);
        }

        return expected.equals(result);
    }

    public boolean compareRoundtripResults(Object expected, Object result)
    {
        return compareResults(expected, result);
    }

}
