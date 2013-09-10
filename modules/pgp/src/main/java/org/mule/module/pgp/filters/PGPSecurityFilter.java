/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.pgp.filters;

import org.mule.api.EncryptionStrategy;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityContext;
import org.mule.api.security.SecurityException;
import org.mule.api.security.UnauthorisedException;
import org.mule.api.security.UnknownAuthenticationTypeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.pgp.LiteralMessage;
import org.mule.module.pgp.Message;
import org.mule.module.pgp.MessageFactory;
import org.mule.module.pgp.PGPAuthentication;
import org.mule.module.pgp.PGPCryptInfo;
import org.mule.module.pgp.PGPKeyRing;
import org.mule.module.pgp.SignedMessage;
import org.mule.module.pgp.i18n.PGPMessages;
import org.mule.security.AbstractEndpointSecurityFilter;

import org.bouncycastle.openpgp.PGPPublicKey;

public class PGPSecurityFilter extends AbstractEndpointSecurityFilter
{
    private EncryptionStrategy strategy;

    private String strategyName;

    private boolean signRequired;

    private PGPKeyRing keyManager;

    @Override
    protected void authenticateInbound(MuleEvent event)
        throws SecurityException, UnauthorisedException, UnknownAuthenticationTypeException
    {
        MuleMessage message = event.getMessage();

        String userId = (String)getCredentialsAccessor().getCredentials(event);

        byte[] creds = null;
        try
        {
            creds = message.getPayloadAsBytes();
            creds = strategy.decrypt(creds, null);
        }
        catch (Exception e1)
        {
            throw new UnauthorisedException(CoreMessages.failedToReadPayload(), event, e1);
        }

        Authentication authentication;
        try
        {
            authentication = new PGPAuthentication(userId, decodeMsgRaw(creds), event);
        }
        catch (Exception e1)
        {
            throw new UnauthorisedException(CoreMessages.failedToReadPayload(), event, e1);
        }

        final Authentication authResult;
        try
        {
            authResult = getSecurityManager().authenticate(authentication);
        }
        catch (Exception e)
        {
            // Authentication failed
            if (logger.isDebugEnabled())
            {
                logger.debug("Authentication request for user: " + userId + " failed: " + e.toString());
            }

            throw new UnauthorisedException(CoreMessages.authFailedForUser(userId), event, e);
        }

        // Authentication success
        if (logger.isDebugEnabled())
        {
            logger.debug("Authentication success: " + authResult.toString());
        }

        SecurityContext context = getSecurityManager().createSecurityContext(authResult);
        event.getSession().setSecurityContext(context);

        try
        {
            updatePayload(message, getUnencryptedMessageWithoutSignature((PGPAuthentication)authResult), event);
//            TODO RequestContext.rewriteEvent(new DefaultMuleMessage(
//                getUnencryptedMessageWithoutSignature((PGPAuthentication)authResult)));
        }
        catch (Exception e2)
        {
            throw new UnauthorisedException(event, context, this);
        }
    }

    private Message decodeMsgRaw(byte[] raw) throws Exception
    {
        return MessageFactory.getMessage(raw);
    }

    private String getUnencryptedMessageWithoutSignature(PGPAuthentication auth) throws Exception
    {
        Message msg = (Message)auth.getCredentials();

        if (msg instanceof SignedMessage)
        {
            msg = ((SignedMessage)msg).getContents();
        }

        if (msg instanceof LiteralMessage)
        {
            return ((LiteralMessage)msg).getTextData();
        }
        else
        {
            throw new Exception("Wrong data");
        }
    }

    @Override
    protected void authenticateOutbound(MuleEvent event) throws SecurityException, UnauthorisedException
    {
        logger.debug("authenticateOutbound:" + event.getId());

        if (!isAuthenticate())
        {
            return;
        }

        MuleMessage message = event.getMessage();

        PGPPublicKey userKeyBundle = keyManager.getPublicKey((String)getCredentialsAccessor().getCredentials(
            event));

        final PGPCryptInfo cryptInfo = new PGPCryptInfo(userKeyBundle, signRequired);

        try
        {
            updatePayload(event.getMessage(), strategy.encrypt(message.getPayloadAsBytes(), cryptInfo), event);
        }
        catch (Exception e1)
        {
            throw new UnauthorisedException(CoreMessages.failedToReadPayload(), event, e1);
        }
    }

    @Override
    protected void doInitialise() throws InitialisationException
    {
        if (strategyName != null)
        {
            strategy = muleContext.getSecurityManager().getEncryptionStrategy(strategyName);
        }

        if (strategy == null)
        {
            throw new InitialisationException(PGPMessages.encryptionStrategyNotSet(), this);
        }
    }

    public EncryptionStrategy getStrategy()
    {
        return strategy;
    }

    public void setStrategy(EncryptionStrategy strategy)
    {
        this.strategy = strategy;
    }

    public void setStrategyName(String name)
    {
        strategyName = name;
    }

    public boolean isSignRequired()
    {
        return signRequired;
    }

    public void setSignRequired(boolean signRequired)
    {
        this.signRequired = signRequired;
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
