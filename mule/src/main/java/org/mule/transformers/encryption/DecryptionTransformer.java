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
package org.mule.transformers.encryption;

import org.mule.umo.security.CryptoFailureException;

/**
 * <code>EncryptionTransformer</code> will transform an encrypted array of
 * bytes or string into an decrypted array of bytes
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class DecryptionTransformer extends AbstractEncryptionTransformer
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -3691559476636732445L;

    protected byte[] getTransformedBytes(byte[] buffer) throws CryptoFailureException
    {
        return getStrategy().decrypt(buffer, null);
    }
}
