/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;

import org.mule.impl.RequestContext;
import org.mule.tck.testmodels.fruit.InvalidSatsuma;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.transformers.TransformerUtils;

import java.util.Arrays;

public abstract class AbstractTransformerTestCase extends AbstractMuleTestCase
{

    //@Override
    protected void doSetUp() throws Exception
    {
        // setup a dummy context for transformers that are event aware
        RequestContext.setEvent(getTestEvent("test"));
    }

    //@Override
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
        if (this.getRoundTripTransformer() != null)
        {
            Object result = this.getRoundTripTransformer().transform(this.getResultData());
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
            UMOTransformer trans = this.getTransformer();
            UMOTransformer trans2 = this.getRoundTripTransformer();
            Object result =
                    TransformerUtils.applyAllTransformersToObject(Arrays.asList(
                            new UMOTransformer[]{trans, trans2}),
                            getTestData());
            this.compareRoundtripResults(this.getTestData(), result);
        }
    }

    public void doTestBadReturnType(UMOTransformer tran, Object src) throws Exception
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

    protected void doTestClone(UMOTransformer original, UMOTransformer clone) throws Exception
    {
        assertNotSame(original, clone);
    }

    public abstract UMOTransformer getTransformer() throws Exception;

    public abstract UMOTransformer getRoundTripTransformer() throws Exception;

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
            // TODO check if RetroTranslating Mule to JDK 1.4 makes this method
            // available
            // return Arrays.deepEquals((Object[])src, (Object[])result);
        }
        else if (expected instanceof byte[] && result instanceof byte[])
        {
            return Arrays.equals((byte[]) expected, (byte[]) result);
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
