/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer.encryption;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.CryptoFailureException;
import org.mule.api.transformer.Transformer;
import org.mule.security.PasswordBasedEncryptionStrategy;
import org.mule.transformer.AbstractTransformerTestCase;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import static org.junit.Assert.fail;

public class EncryptionTransformerTestCase extends AbstractTransformerTestCase
{
    private static final String TEST_DATA = "the quick brown fox jumped over the lazy dog the quick brown fox jumped over the lazy dog the quick brown fox jumped over the lazy dog";
    
    private PasswordBasedEncryptionStrategy strat;

    @Override
    protected void doSetUp() throws Exception
    {
        strat = new PasswordBasedEncryptionStrategy();
        strat.setPassword("mule");
        strat.initialise();
    }

    @Override
    public Object getResultData()
    {
        try
        {
            return new ByteArrayInputStream(strat.encrypt(TEST_DATA.getBytes(), null));
        }
        catch (CryptoFailureException e)
        {
            fail(e.getMessage());
            return null;
        }
    }

    @Override
    public Object getTestData()
    {
        return new ByteArrayInputStream(TEST_DATA.getBytes());
    }

    @Override
    public Transformer getTransformer()
    {
        EncryptionTransformer transformer = new EncryptionTransformer();
        transformer.setStrategy(strat);
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

    @Override
    public Transformer getRoundTripTransformer()
    {
        DecryptionTransformer transformer = new DecryptionTransformer();
        transformer.setStrategy(strat);
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

    @Override
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
            return Arrays.equals((byte[]) src, (byte[]) result);
        }
        else
        {
            return super.compareResults(src, result);
        }
    }
}
