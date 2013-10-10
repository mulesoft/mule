/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.pgp;

import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityException;
import org.mule.api.security.UnauthorisedException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.pgp.i18n.PGPMessages;
import org.mule.security.AbstractSecurityProvider;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPPublicKey;

public class PGPSecurityProvider extends AbstractSecurityProvider
{
    private PGPKeyRing keyManager;

    public PGPSecurityProvider()
    {
        super("pgp");
    }
    
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
    public boolean supports(Class aClass)
    {
        return PGPAuthentication.class.isAssignableFrom(aClass);
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        try
        {
            java.security.Security.addProvider(new BouncyCastleProvider());
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
