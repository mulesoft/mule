/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.pgp;

import cryptix.pki.KeyBundle;

public class PGPCryptInfo
{
    KeyBundle keyBundle;
    boolean signRequested;

    public PGPCryptInfo(KeyBundle keyBundle, boolean signRequested)
    {
        super();

        this.keyBundle = keyBundle;
        this.signRequested = signRequested;
    }

    public KeyBundle getKeyBundle()
    {
        return keyBundle;
    }

    public void setKeyBundle(KeyBundle keyBundle)
    {
        this.keyBundle = keyBundle;
    }

    public boolean isSignRequested()
    {
        return signRequested;
    }

    public void setSignRequested(boolean signRequested)
    {
        this.signRequested = signRequested;
    }
}
