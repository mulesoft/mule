/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.tck;

import org.mule.tck.testmodels.fruit.InvalidSatsuma;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractTransformerTestCase extends AbstractMuleTestCase
{
    public void testTransform() throws Exception
    {
        doTransform(getTransformer(), getTestData(), getResultData());
    }

    public void testRoundtripTransform() throws Exception
    {
        if (getRoundTripTransformer() != null)
        {
            doTransform(getRoundTripTransformer(), getResultData(), getTestData());
        }
    }

    public void testBadReturnType() throws Exception
    {
        doTestBadReturnType(getTransformer(), getTestData());
    }

    public void testRoundtripBadReturnType() throws Exception
    {
        if (getRoundTripTransformer() != null)
        {
            doTestBadReturnType(getRoundTripTransformer(), getResultData());
        }
    }

    public void testRoundTrip() throws Exception
    {
        if (getRoundTripTransformer() != null)
        {
            UMOTransformer trans = getTransformer();
            UMOTransformer trans2 = getRoundTripTransformer();
            trans.setTransformer(trans2);
            Object result = trans.transform(getTestData());
            compareResults(getTestData(), result);
        }
    }

    public void doTransform(UMOTransformer trans, Object src, Object expectedResult) throws Exception
    {
        Object result = trans.transform(src);
        assertNotNull(result);
        assertTrue(compareResults(expectedResult, result));
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

    public abstract UMOTransformer getTransformer() throws Exception;

    public abstract UMOTransformer getRoundTripTransformer() throws Exception;

    public abstract Object getTestData();

    public abstract Object getResultData();

    public boolean compareResults(Object src, Object result) {
        if(src==null && result ==null) return true;
        if(src==null || result ==null) return false;
        return src.equals(result);
    }

}
