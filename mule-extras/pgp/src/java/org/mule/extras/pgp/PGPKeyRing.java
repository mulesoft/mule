/*
 * Project: mule-extras-pgp
 * Author : ariva
 * Created on 31-mar-2005
 *
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