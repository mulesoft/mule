/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.mule.security;

import org.mule.security.SecretKeyEncryptionStrategy;
import org.mule.security.SecretKeyFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class SecretKeyEncryptionStrategyTestCase extends AbstractMuleTestCase
{

    @Test
    public void testRoundTripEncryptionBlowfish() throws Exception
    {
        SecretKeyEncryptionStrategy ske = new SecretKeyEncryptionStrategy();
        ske.setAlgorithm("Blowfish");
        ske.setKey("shhhhh");
        ske.initialise();

        byte[] b = ske.encrypt("hello".getBytes(), null);

        assertNotSame(new String(b), "hello");
        String s = new String(ske.decrypt(b, null), "UTF-8");
        assertEquals("hello", s);
    }

    @Test
    public void testRoundTripEncryptionBlowfishWithKeyFactory() throws Exception
    {
        SecretKeyEncryptionStrategy ske = new SecretKeyEncryptionStrategy();
        ske.setAlgorithm("Blowfish");
        ske.setKeyFactory(new SecretKeyFactory()
        {
            public byte[] getKey()
            {
                return "shhhh".getBytes();
            }
        });
        ske.initialise();

        byte[] b = ske.encrypt("hello".getBytes(), null);

        assertNotSame(new String(b), "hello");
        String s = new String(ske.decrypt(b, null), "UTF-8");
        assertEquals("hello", s);
    }

    @Test
    public void testRoundTripEncryptionTripleDES() throws Exception
    {
        SecretKeyEncryptionStrategy ske = new SecretKeyEncryptionStrategy();
        ske.setAlgorithm("TripleDES");
        ske.setKey("shhhhh");

        ske.initialise();

        byte[] b = ske.encrypt("hello".getBytes(), null);

        assertNotSame(new String(b), "hello");
        String s = new String(ske.decrypt(b, null), "UTF-8");
        assertEquals("hello", s);
    }

}
