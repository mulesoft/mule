/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.extras.pgp;

import cryptix.pki.KeyBundle;

/**
 * @author ariva
 * 
 */
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
