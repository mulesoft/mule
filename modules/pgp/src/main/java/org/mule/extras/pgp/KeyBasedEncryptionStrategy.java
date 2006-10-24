/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.pgp;

import cryptix.message.EncryptedMessage;
import cryptix.message.EncryptedMessageBuilder;
import cryptix.message.LiteralMessageBuilder;
import cryptix.message.Message;
import cryptix.message.MessageFactory;
import cryptix.message.SignedMessageBuilder;
import cryptix.openpgp.PGPArmouredMessage;
import cryptix.pki.KeyBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.security.CryptoFailureException;

import java.io.ByteArrayInputStream;
import java.util.Collection;

/**
 * @author ariva
 */
public class KeyBasedEncryptionStrategy implements UMOEncryptionStrategy
{
    protected static transient Log logger = LogFactory.getLog(KeyBasedEncryptionStrategy.class);

    private PGPKeyRing keyManager;

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOEncryptionStrategy#encrypt(byte[])
     */
    public byte[] encrypt(byte[] data, Object cryptInfo) throws CryptoFailureException
    {
        try
        {
            PGPCryptInfo pgpCryptInfo = (PGPCryptInfo)cryptInfo;
            KeyBundle publicKey = pgpCryptInfo.getKeyBundle();

            LiteralMessageBuilder lmb = LiteralMessageBuilder.getInstance("OpenPGP");

            lmb.init(data);

            Message msg = lmb.build();

            if (pgpCryptInfo.isSignRequested())
            {
                SignedMessageBuilder smb = SignedMessageBuilder.getInstance("OpenPGP");

                smb.init(msg);
                smb.addSigner(keyManager.getSecretKeyBundle(), keyManager.getSecretPassphrase().toCharArray());

                msg = smb.build();
            }

            EncryptedMessageBuilder emb = EncryptedMessageBuilder.getInstance("OpenPGP");
            emb.init(msg);
            emb.addRecipient(publicKey);
            msg = emb.build();

            return new PGPArmouredMessage(msg).getEncoded();
        }
        catch (Exception e)
        {
            throw new CryptoFailureException(this, e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOEncryptionStrategy#decrypt(byte[])
     */
    public byte[] decrypt(byte[] data, Object cryptInfo) throws CryptoFailureException
    {
        try
        {
            MessageFactory mf = MessageFactory.getInstance("OpenPGP");

            ByteArrayInputStream in = new ByteArrayInputStream(data);

            Collection msgs = mf.generateMessages(in);

            Message msg = (Message)msgs.iterator().next();

            if (msg instanceof EncryptedMessage)
            {
                msg = ((EncryptedMessage)msg).decrypt(keyManager.getSecretKeyBundle(),
                    keyManager.getSecretPassphrase().toCharArray());

                return new PGPArmouredMessage(msg).getEncoded();
            }
        }
        catch (Exception e)
        {
            throw new CryptoFailureException(this, e);
        }

        return data;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.Initialisable#initialise()
     */
    public void initialise() throws InitialisationException
    {
        try
        {
            java.security.Security.addProvider(new cryptix.jce.provider.CryptixCrypto());
            java.security.Security.addProvider(new cryptix.openpgp.provider.CryptixOpenPGP());
        }
        catch (Exception e)
        {
            throw new InitialisationException(new org.mule.config.i18n.Message(Messages.FAILED_TO_CREATE_X,
                "KeyBasedEncryptionStrategy"), e, this);
        }
    }

    public PGPKeyRing getKeyManager()
    {
        return keyManager;
    }

    public void setKeyManager(PGPKeyRing keyManager)
    {
        this.keyManager = keyManager;
    }
}
