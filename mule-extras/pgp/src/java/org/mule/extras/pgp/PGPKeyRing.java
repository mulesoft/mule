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
 */
package org.mule.extras.pgp;

import cryptix.pki.KeyBundle;

/**
 * @author ariva
 *
 */
public interface PGPKeyRing {
    public abstract String getSecretPassphrase();

    public abstract KeyBundle getSecretKeyBundle();

    public abstract KeyBundle getKeyBundle(String principalId);
}