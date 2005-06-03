/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.mule.security;

import org.mule.impl.security.PasswordBasedEncryptionStrategy;
import org.mule.tck.NamedTestCase;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class PbeEncryptionStrategyTestCase extends NamedTestCase
{
    public void testRoundTripEncryption() throws Exception
    {
        PasswordBasedEncryptionStrategy pbe = new PasswordBasedEncryptionStrategy();
        pbe.setPassword("test");
        pbe.initialise();

        byte[] b = pbe.encrypt("hello".getBytes(), null);

        String s = new String(pbe.decrypt(b, null), "UTF-8");
        assertEquals("hello", s);
    }

    public void testRoundTripEncryptionWithCharsetConversion() throws Exception
    {
        // PasswordBasedEncryptionStrategy pbe = new
        // PasswordBasedEncryptionStrategy();
        // pbe.setPassword("test");
        // pbe.initialise();
        //
        // CharsetEncoder ce = new UnicodeEncoder(Charset)
        // {
        // protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out)
        // {
        // return null;
        // }
        // };
        // byte[] b = pbe.encrypt("hello".getBytes());
        //
        // String s1 = new String(b);
        //
        // String s= new String(pbe.decrypt(s1.getBytes()));
        // assertEquals("hello", s);
    }
}
