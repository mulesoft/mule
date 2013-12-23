/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityException;
import org.mule.api.security.UnauthorisedException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.pgp.i18n.PGPMessages;
import org.mule.security.AbstractSecurityProvider;
import org.mule.util.SecurityUtils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPPublicKey;

public class PGPSecurityProvider extends AbstractSecurityProvider
{
    private PGPKeyRing keyManager;

    public PGPSecurityProvider()
    {
        super("pgp");
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws SecurityException
    {
        PGPAuthentication auth = (PGPAuthentication) authentication;

        String userId = (String) auth.getPrincipal();

        if (userId == null)
        {
            throw new UnauthorisedException(CoreMessages.objectIsNull("UserId"));
        }

        PGPPublicKey publicKey = keyManager.getPublicKey(userId);

        if (publicKey == null)
        {
            throw new UnauthorisedException(PGPMessages.noPublicKeyForUser(userId));
        }

        Message msg = (Message) auth.getCredentials();

        if (msg instanceof SignedMessage)
        {
            try
            {
                if (!((SignedMessage) msg).verify())
                {
                    throw new UnauthorisedException(PGPMessages.invalidSignature());
                }
            }
            catch (Exception e)
            {
                throw new UnauthorisedException(PGPMessages.errorVerifySignature(), e);
            }
        }

        auth.setAuthenticated(true);
        auth.setDetails(publicKey);

        return auth;
    }

    @Override
    public boolean supports(Class<?> aClass)
    {
        return PGPAuthentication.class.isAssignableFrom(aClass);
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        try
        {
            if (!SecurityUtils.isFipsSecurityModel())
            {
                java.security.Security.addProvider(new BouncyCastleProvider());
            }
            setSecurityContextFactory(new PGPSecurityContextFactory());
        }
        catch (Exception e)
        {
            throw new InitialisationException(CoreMessages.failedToCreate("PGPProvider"), e, this);
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
