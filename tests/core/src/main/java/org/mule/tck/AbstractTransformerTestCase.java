/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck;

import java.util.Arrays;

import org.mule.impl.RequestContext;
import org.mule.tck.testmodels.fruit.InvalidSatsuma;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractTransformerTestCase extends AbstractMuleTestCase
{
    protected void doSetUp() throws Exception
    {
        // setup a dummy context for transformers that are event aware
        RequestContext.setEvent(getTestEvent("test"));
    }

    protected void doTearDown() throws Exception
    {
        RequestContext.setEvent(null);
    }

    protected  String normalizeString(String rawString) {
        return rawString.replaceAll("\r\n", "\n");
    }

    public void testTransform() throws Exception
    {
        Object result = getTransformer().transform(getTestData());
        assertNotNull(result);

        Object expectedResult = getResultData();
        // Special case for string results
        if (result instanceof String && expectedResult instanceof String ) {
            assertEquals(normalizeString((String)expectedResult), normalizeString((String)result));
        } else {
            boolean b = compareResults(expectedResult, result);
            assertTrue(b);
        }
    }

    public void testRoundtripTransform() throws Exception
    {
        if (getRoundTripTransformer() != null) {
            Object result = getRoundTripTransformer().transform(getResultData());
            assertNotNull(result);
            boolean b = compareRoundtripResults(getTestData(), result);
            assertTrue(b);
        }
    }

    public void testBadReturnType() throws Exception
    {
        doTestBadReturnType(getTransformer(), getTestData());
    }

    public void testRoundtripBadReturnType() throws Exception
    {
        if (getRoundTripTransformer() != null) {
            doTestBadReturnType(getRoundTripTransformer(), getResultData());
        }
    }

    public void testRoundTrip() throws Exception
    {
        if (getRoundTripTransformer() != null) {
            UMOTransformer trans = getTransformer();
            trans.setNextTransformer(getRoundTripTransformer());
            Object result = trans.transform(getTestData());
            compareRoundtripResults(getTestData(), result);
        }
    }

    public void doTestBadReturnType(UMOTransformer tran, Object src) throws Exception
    {
        tran.setReturnClass(InvalidSatsuma.class);
        try {
            tran.transform(src);
            fail("Should throw exception for bad return type");
        } catch (TransformerException e) {
            // expected
        }
    }

    public abstract UMOTransformer getTransformer() throws Exception;

    public abstract UMOTransformer getRoundTripTransformer() throws Exception;

    public abstract Object getTestData();

    public abstract Object getResultData();

    public boolean compareResults(Object src, Object result)
    {
        if (src == null && result == null) {
            return true;
        }
        if (src == null || result == null) {
            return false;
        }
        if(src instanceof Object[] && result instanceof Object[] ) {
            return Arrays.equals((Object[])src, (Object[])result);
            // TODO check if RetroTranslating Mule to JDK 1.4 makes this method available
            //return Arrays.deepEquals((Object[])src, (Object[])result);
        } else if (src instanceof byte[] && result instanceof byte[] ) {
            return Arrays.equals((byte[])src, (byte[])result);
        } else {
            return src.equals(result);
        }
    }

    public boolean compareRoundtripResults(Object src, Object result)
    {
        return compareResults(src, result);
    }
}
