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
package org.mule.extras.pgp;

import cryptix.pki.ExtendedKeyStore;
import cryptix.pki.KeyBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Messages;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.Utility;

import java.io.InputStream;
import java.security.Principal;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author ariva
 * 
 */
public class PGPKeyRingImpl implements PGPKeyRing
{
    protected static transient Log logger = LogFactory.getLog(PGPKeyRingImpl.class);

    private String publicKeyRingFileName;

    private HashMap principalsKeyBundleMap;

    private String secretKeyRingFileName;

    private String secretAliasId;

    private KeyBundle secretKeyBundle;

    private String secretPassphrase;

    public PGPKeyRingImpl()
    {
    }

    public String getSecretKeyRingFileName()
    {
        return secretKeyRingFileName;
    }

    public void setSecretKeyRingFileName(String value)
    {
        this.secretKeyRingFileName = value;
    }

    public String getSecretAliasId()
    {
        return secretAliasId;
    }

    public void setSecretAliasId(String value)
    {
        this.secretAliasId = value;
    }

    public String getSecretPassphrase()
    {
        return secretPassphrase;
    }

    public void setSecretPassphrase(String value)
    {
        this.secretPassphrase = value;
    }

    private void readPrivateKeyBundle() throws Exception
    {
        InputStream in = Utility.loadResource(secretKeyRingFileName, getClass());

        ExtendedKeyStore ring = (ExtendedKeyStore) ExtendedKeyStore.getInstance("OpenPGP/KeyRing");
        ring.load(in, null);

        in.close();

        secretKeyBundle = ring.getKeyBundle(secretAliasId);
    }

    public KeyBundle getSecretKeyBundle()
    {
        return secretKeyBundle;
    }

    /**
     * @return
     */
    public String getPublicKeyRingFileName()
    {
        return publicKeyRingFileName;
    }

    /**
     * @param value
     */
    public void setPublicKeyRingFileName(String value)
    {
        this.publicKeyRingFileName = value;
    }

    public KeyBundle getKeyBundle(String principalId)
    {
        return (KeyBundle) principalsKeyBundleMap.get(principalId);
    }

    public void initialise() throws InitialisationException
    {
        try {
            java.security.Security.addProvider(new cryptix.jce.provider.CryptixCrypto());
            java.security.Security.addProvider(new cryptix.openpgp.provider.CryptixOpenPGP());

            principalsKeyBundleMap = new HashMap();

            readPublicKeyRing();
            readPrivateKeyBundle();
        } catch (Exception e) {
            logger.error("errore in inizializzazione:" + e.getMessage(), e);
            throw new InitialisationException(new org.mule.config.i18n.Message(Messages.FAILED_TO_CREATE_X,
                                                                               "PGPKeyRingImpl"), e);
        }
    }

    private void readPublicKeyRing() throws Exception
    {
        logger.debug(System.getProperties().get("user.dir"));
        InputStream in = Utility.loadResource(publicKeyRingFileName, getClass());

        ExtendedKeyStore ring = (ExtendedKeyStore) ExtendedKeyStore.getInstance("OpenPGP/KeyRing");
        ring.load(in, null);
        in.close();

        for (Enumeration e = ring.aliases(); e.hasMoreElements();) {
            String aliasId = (String) e.nextElement();

            KeyBundle bundle = ring.getKeyBundle(aliasId);

            if (bundle != null) {
                for (Iterator users = bundle.getPrincipals(); users.hasNext();) {
                    Principal princ = (Principal) users.next();

                    principalsKeyBundleMap.put(princ.getName(), bundle);
                }
            }
        }
    }
}
