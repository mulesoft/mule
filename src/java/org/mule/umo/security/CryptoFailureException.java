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
package org.mule.umo.security;

import org.mule.MuleException;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.umo.UMOEncryptionStrategy;

/**
 * <code>CryptoFailureException</code> is a generic exception thrown by an
 * CryptoStrategy if encryption or decryption fails. The constuctors of this
 * exception accept a UMOEncryptionStrategy that will be included in the
 * exception message. Implementors of UMOEncryptionStrategy should provide a
 * toString method that exposes *only* information that maybe useful for
 * debugging not passwords, secret keys, etc.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class CryptoFailureException extends MuleException
{
    private transient UMOEncryptionStrategy encryptionStrategy;

    public CryptoFailureException(Message message, UMOEncryptionStrategy strategy)
    {
        super(message);
        String s = (strategy == null ? "null" : strategy.toString());
        addInfo("Encryption", s);
        this.encryptionStrategy = strategy;
    }

    public CryptoFailureException(Message message, UMOEncryptionStrategy strategy, Throwable cause)
    {
        super(message, cause);
        String s = (strategy == null ? "null" : strategy.toString());
        addInfo("Encryption", s);
        this.encryptionStrategy = strategy;
    }

    public CryptoFailureException(UMOEncryptionStrategy strategy, Throwable cause)
    {
        super(new Message(Messages.CRYPTO_FAILURE), cause);
        String s = (strategy == null ? "null" : strategy.toString());
        addInfo("Encryption", s);
        this.encryptionStrategy = strategy;

    }

    public UMOEncryptionStrategy getEncryptionStrategy()
    {
        return encryptionStrategy;
    }
}
