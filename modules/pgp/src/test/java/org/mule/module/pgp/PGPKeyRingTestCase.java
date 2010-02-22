/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.pgp;

import cryptix.pki.KeyBundle;

public class PGPKeyRingTestCase extends AbstractEncryptionStrategyTestCase
{
    public void testClientKey()
    {
        KeyBundle clientKey = keyManager.getKeyBundle("Mule client <mule_client@mule.com>");
        assertNotNull(clientKey);
    }

    public void testServerKey()
    {
        KeyBundle serverKey = keyManager.getSecretKeyBundle();
        assertNotNull(serverKey);
    }
}
