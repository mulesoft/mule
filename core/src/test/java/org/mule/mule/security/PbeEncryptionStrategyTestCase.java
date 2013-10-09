/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.mule.security;

import org.mule.security.PasswordBasedEncryptionStrategy;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class PbeEncryptionStrategyTestCase extends AbstractMuleTestCase
{

    @Test
    public void testRoundTripEncryption() throws Exception
    {
        PasswordBasedEncryptionStrategy pbe = new PasswordBasedEncryptionStrategy();
        pbe.setPassword("test");
        pbe.initialise();

        byte[] b = pbe.encrypt("hello".getBytes(), null);

        assertNotSame(new String(b), "hello");
        String s = new String(pbe.decrypt(b, null), "UTF-8");
        assertEquals("hello", s);
    }
}
