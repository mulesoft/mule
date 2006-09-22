/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.mule.security;

import org.mule.impl.security.SecretKeyEncryptionStrategy;
import org.mule.tck.AbstractMuleTestCase;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class SecretKeyEncryptionStrategyTestCase extends AbstractMuleTestCase
{
    public void testRoundTripEncryptionBlowfish() throws Exception
    {
        SecretKeyEncryptionStrategy ske = new SecretKeyEncryptionStrategy();
        ske.setAlgorithm("Blowfish");
        ske.setkey("shhhhh");
        ske.initialise();

        byte[] b = ske.encrypt("hello".getBytes(), null);

        assertNotSame(new String(b), "hello");
        String s = new String(ske.decrypt(b, null), "UTF-8");
        assertEquals("hello", s);
    }

    public void testRoundTripEncryptionTripleDES() throws Exception
    {
        SecretKeyEncryptionStrategy ske = new SecretKeyEncryptionStrategy();
        ske.setAlgorithm("TripleDES");
        ske.setkey("shhhhh");

        ske.initialise();

        byte[] b = ske.encrypt("hello".getBytes(), null);

        assertNotSame(new String(b), "hello");
        String s = new String(ske.decrypt(b, null), "UTF-8");
        assertEquals("hello", s);
    }
}
