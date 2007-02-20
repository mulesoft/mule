/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.transformers.encryption;

import org.mule.impl.security.PasswordBasedEncryptionStrategy;
import org.mule.tck.AbstractTransformerTestCase;
import org.mule.transformers.encryption.DecryptionTransformer;
import org.mule.transformers.encryption.EncryptionTransformer;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.security.CryptoFailureException;
import org.mule.umo.transformer.UMOTransformer;

import java.util.Arrays;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class EncryptionTransformerTestCase extends AbstractTransformerTestCase
{
    private PasswordBasedEncryptionStrategy strat;

    protected void doSetUp() throws Exception
    {
        strat = new PasswordBasedEncryptionStrategy();
        strat.setPassword("mule");
        strat.initialise(managementContext);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.tck.AbstractTransformerTestCase#getResultData()
     */
    public Object getResultData()
    {
        try
        {
            return strat.encrypt(getTestData().toString().getBytes(), null);
        }
        catch (CryptoFailureException e)
        {
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
        EncryptionTransformer transformer = new EncryptionTransformer();
        transformer.setStrategy(strat);
        try
        {
            transformer.initialise(managementContext);
        }
        catch (InitialisationException e)
        {
            fail(e.getMessage());
        }
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
        DecryptionTransformer transformer = new DecryptionTransformer();
        transformer.setStrategy(strat);
        transformer.setReturnClass(String.class);
        try
        {
            transformer.initialise(managementContext);
        }
        catch (InitialisationException e)
        {
            fail(e.getMessage());
        }
        return transformer;
    }

    public boolean compareResults(Object src, Object result)
    {
        if (src == null && result == null)
        {
            return true;
        }

        if (src == null || result == null)
        {
            return false;
        }

        if (src instanceof byte[] && result instanceof byte[])
        {
            return Arrays.equals((byte[])src, (byte[])result);
        }
        else
        {
            return super.compareResults(src, result);
        }
    }

}
