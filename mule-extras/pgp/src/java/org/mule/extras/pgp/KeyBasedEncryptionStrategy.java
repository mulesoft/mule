/*
 * Project: mule-extras-pgp
 * Author : ariva
 * Created on 22-mar-2005
 *
 */
package org.mule.extras.pgp;

import cryptix.message.EncryptedMessage;
import cryptix.message.Message;
import cryptix.message.MessageFactory;
import cryptix.pki.ExtendedKeyStore;
import cryptix.pki.KeyBundle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.security.CryptoFailureException;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.Collection;

/**
 * @author ariva
 *
 */
public class KeyBasedEncryptionStrategy implements UMOEncryptionStrategy {

    protected static transient Log logger = LogFactory.getLog(KeyBasedEncryptionStrategy.class);
    
	private String secretKeyRingFileName;
	private String secretAliasId;
	private KeyBundle secretKeyBundle;
	private String secretPassphrase;

	public String getSecretKeyRingFileName() {
		return secretKeyRingFileName;
	}
	public void setSecretKeyRingFileName(String value) {
		this.secretKeyRingFileName=value;
	}

	public String getSecretAliasId() {
		return secretAliasId;
	}
	public void setSecretAliasId(String value) {
		this.secretAliasId=value;
	}

	public String getSecretPassphrase() {
		return secretPassphrase;
	}
	public void setSecretPassphrase(String value) {
		this.secretPassphrase=value;
	}
	
    /* (non-Javadoc)
     * @see org.mule.umo.UMOEncryptionStrategy#encrypt(byte[])
     */
    public byte[] encrypt(byte[] data, Object cryptInfo) throws CryptoFailureException {
        // TODO
        // This interface don't support asymmetric key pairs.
        // Message should be encrypted using the receiver public key, not our private key!
        // So here I need to know also the addressee and get the public key (from PGPSecurityProvider)
        
        return data;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.UMOEncryptionStrategy#decrypt(byte[])
     */
    public byte[] decrypt(byte[] data, Object cryptInfo) throws CryptoFailureException {
        try {
            MessageFactory mf = MessageFactory.getInstance("OpenPGP");
            
            ByteArrayInputStream in=new ByteArrayInputStream( data );
            
            Collection msgs=mf.generateMessages(in);

            Message msg=(Message)msgs.iterator().next();

			if (msg instanceof EncryptedMessage) {
				msg=((EncryptedMessage)msg).decrypt(secretKeyBundle, secretPassphrase.toCharArray());
				return msg.getEncoded();
			}
        } catch (Exception e) {            
            throw new CryptoFailureException(this, e);
        }
        
	    return data;
    }

    /* (non-Javadoc)
     * @see org.mule.umo.lifecycle.Initialisable#initialise()
     */
    public void initialise() throws InitialisationException {
		try
		{
			java.security.Security.addProvider(
					new cryptix.jce.provider.CryptixCrypto() );
			java.security.Security.addProvider(
					new cryptix.openpgp.provider.CryptixOpenPGP() );

			readPrivateKeyBundle();

		} catch (Exception e) {
			throw new InitialisationException(new org.mule.config.i18n.Message(Messages.FAILED_TO_CREATE_X, "KeyBasedEncryptionStrategy"), e, this);
		}
    }

	private void readPrivateKeyBundle() throws Exception {
		FileInputStream in = new FileInputStream(secretKeyRingFileName);

		ExtendedKeyStore ring = (ExtendedKeyStore) ExtendedKeyStore.getInstance("OpenPGP/KeyRing");
		ring.load(in, null);

		in.close();

		secretKeyBundle=ring.getKeyBundle(secretAliasId);
	}

}
