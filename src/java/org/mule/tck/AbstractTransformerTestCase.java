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
 * <p><code>AbstractTransformerTestCase</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractTransformerTestCase extends AbstractMuleTestCase
{
    public void testSessionTransform() throws Exception
    {
        doTestSessionTransform(getTransformer(), getTestData(), getResultData());
    }

    public void testRoundTripSessionTransform() throws Exception
    {
        if (getRoundTripTransformer() != null)
        {
            doTestSessionTransform(getRoundTripTransformer(), getResultData(), getTestData());
        }
    }

    public void testAutoCommitTransform() throws Exception
    {
        doTestAutoCommitTransform(getTransformer(), getTestData(), getResultData());
    }

    public void testRoundtripAutoCommitTransform() throws Exception
    {
        if (getRoundTripTransformer() != null)
        {
            doTestAutoCommitTransform(getRoundTripTransformer(), getResultData(), getTestData());
        }
    }

    public void testBadSessionCalls() throws Exception
    {
        doTestBadSessionCalls(getTransformer());
    }

    public void testRoundTripBadSessionCalls() throws Exception
    {
        if (getRoundTripTransformer() != null)
        {
            doTestBadSessionCalls(getRoundTripTransformer());
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
//            UMOTransformer trans = getTransformer();
//            UMOTransformer trans2 = getRoundTripTransformer();
//            trans.setAutoCommit(false);
//            assertTrue(!trans.isInSession());
//            trans.setTransformer(trans2);
//            trans.startSession();
//            trans.transform(getTestData());
//            Object result = trans.commitSession();
//            compareResults(getTestData(), result);
        }
    }

    public void doTestSessionTransform(UMOTransformer trans, Object src, Object expectedResult) throws Exception
    {
//        trans.setAutoCommit(false);
//        assertTrue(!trans.isInSession());
//        trans.startSession();
//        Object result = trans.transform(src);
//        assertNotNull(result);
//        assertTrue(!result.equals(src));
//        assertTrue(compareResults(expectedResult, result));
//        result = trans.rollbackSession();
//        assertEquals(src, result);
//        assertTrue(!trans.isInSession());
//
//        result = trans.sessionTransform(src);
//        assertTrue(compareResults(expectedResult, result));
//        assertTrue(trans.isInSession());
//        Object result2 = trans.commitSession();
//        assertEquals(result, result2);
//        assertTrue(!trans.isInSession());
    }

    public void doTestAutoCommitTransform(UMOTransformer trans, Object src, Object expectedResult) throws Exception
    {
//        trans.setAutoCommit(true);
//        assertTrue(trans.isAutoCommit());
//
//        assertTrue(!trans.isInSession());
//        trans.startSession();
//        Object result = trans.transform(src);
//        assertNotNull(result);
//        assertTrue(compareResults(expectedResult, result));
//        assertTrue(!trans.isInSession());
    }

    public void doTestBadSessionCalls(UMOTransformer tran) throws Exception
    {
//        tran.startSession();
//        assertTrue(tran.isInSession());
//        try
//        {
//            tran.startSession();
//            fail("Should throw exception session is already in a transaction");
//        }
//        catch (TransformerException e)
//        {
//            // expected
//        }
//
//        tran.rollbackSession();
//        try
//        {
//            tran.rollbackSession();
//            fail("Transformer should not be in a session");
//        }
//        catch (TransformerException e1)
//        {
//            // expected
//        }
//        try
//        {
//            tran.commitSession();
//            fail("Transformer should not be in a session");
//        }
//        catch (TransformerException e2)
//        {
//            // expected
//        }
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
