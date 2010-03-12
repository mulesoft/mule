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

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.CredentialsAccessor;
import org.mule.api.security.CryptoFailureException;
import org.mule.config.i18n.CoreMessages;
import org.mule.security.AbstractNamedEncryptionStrategy;

import cryptix.message.EncryptedMessage;
import cryptix.message.EncryptedMessageBuilder;
import cryptix.message.LiteralMessageBuilder;
import cryptix.message.Message;
import cryptix.message.MessageFactory;
import cryptix.message.SignedMessageBuilder;
import cryptix.openpgp.PGPArmouredMessage;
import cryptix.openpgp.PGPDetachedSignatureMessage;
import cryptix.openpgp.PGPSignedMessage;
import cryptix.openpgp.packet.PGPSignaturePacket;
import cryptix.openpgp.provider.PGPDetachedSignatureMessageImpl;
import cryptix.pki.KeyBundle;

import java.io.ByteArrayInputStream;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class KeyBasedEncryptionStrategy extends AbstractNamedEncryptionStrategy
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(KeyBasedEncryptionStrategy.class);

    private PGPKeyRing keyManager;
    private CredentialsAccessor credentialsAccessor;

    public byte[] encrypt(byte[] data, Object cryptInfo) throws CryptoFailureException
    {
        try
        {
            PGPCryptInfo pgpCryptInfo;
            KeyBundle publicKey;
            
            if (cryptInfo == null)
            {
                MuleEvent event = RequestContext.getEvent();
                publicKey = keyManager.getKeyBundle((String)credentialsAccessor.getCredentials(
                    event));
                
                pgpCryptInfo = new PGPCryptInfo(publicKey, false);
            }
            else
            {
                pgpCryptInfo = (PGPCryptInfo)cryptInfo;
                publicKey = pgpCryptInfo.getKeyBundle();
            }

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

    public byte[] decrypt(byte[] data, Object cryptInfo) throws CryptoFailureException
    {
        try
        {
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            MessageFactory mf = MessageFactory.getInstance("OpenPGP");
            Collection<?> msgs = mf.generateMessages(in);
            Message msg = (Message) msgs.iterator().next();

            if (msg instanceof EncryptedMessage)
            {
                EncryptedMessage encryptedMessage = (EncryptedMessage) msg;
                KeyBundle secretKeyBundle = keyManager.getSecretKeyBundle();
                char[] passphrase = keyManager.getSecretPassphrase().toCharArray();
                msg = encryptedMessage.decrypt(secretKeyBundle, passphrase);
                
                applyStrongEncryptionWorkaround(msg);

                return new PGPArmouredMessage(msg).getEncoded();
            }
        }
        catch (Exception e)
        {
            throw new CryptoFailureException(this, e);
        }

        return data;
    }

    // cryptix seems to have trouble with some kinds of messsage encryption. Work around this
    // by setting up the proper internal state first
    private void applyStrongEncryptionWorkaround(Message msg) throws Exception
    {
        if (msg instanceof PGPSignedMessage)
        {
            PGPSignedMessage signedMessage = (PGPSignedMessage) msg;
            
            PGPDetachedSignatureMessage signature = signedMessage.getDetachedSignature();
            if (signature instanceof PGPDetachedSignatureMessageImpl)
            {
                PGPDetachedSignatureMessageImpl signatureImpl = 
                    (PGPDetachedSignatureMessageImpl) signature;
                PGPSignaturePacket packet = signatureImpl.getPacket();
                if (packet.getVersion() == 4)
                {
                    packet.parseSignatureSubPackets();
                }
            }
        }
    }

    public void initialise() throws InitialisationException
    {
        try
        {
            java.security.Security.addProvider(new cryptix.jce.provider.CryptixCrypto());
            java.security.Security.addProvider(new cryptix.openpgp.provider.CryptixOpenPGP());
        }
        catch (Exception e)
        {
            throw new InitialisationException(
                CoreMessages.failedToCreate("KeyBasedEncryptionStrategy"), e, this);
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

    public CredentialsAccessor getCredentialsAccessor() {
        return credentialsAccessor;
    }

    public void setCredentialsAccessor(CredentialsAccessor credentialsAccessor) {
        this.credentialsAccessor = credentialsAccessor;
    }
}
