/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
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
import org.mule.api.security.UnauthorisedException;
import org.mule.api.security.UnknownAuthenticationTypeException;
import org.mule.config.i18n.CoreMessages;
import org.mule.module.pgp.PGPAuthentication;
import org.mule.module.pgp.PGPCryptInfo;
import org.mule.module.pgp.PGPKeyRing;
import org.mule.module.pgp.i18n.PGPMessages;
import org.mule.security.AbstractEndpointSecurityFilter;

import java.io.ByteArrayInputStream;
import java.util.Collection;

import cryptix.message.LiteralMessage;
import cryptix.message.Message;
import cryptix.message.MessageFactory;
import cryptix.message.SignedMessage;
import cryptix.pki.KeyBundle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PGPSecurityFilter extends AbstractEndpointSecurityFilter
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(PGPSecurityFilter.class);

    private EncryptionStrategy strategy;

    private String strategyName;

    private boolean signRequired;

    private PGPKeyRing keyManager;

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.security.AbstractEndpointSecurityFilter#authenticateInbound(org.mule.api.MuleEvent)
     */
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
            throw new UnauthorisedException(
                CoreMessages.failedToReadPayload(), event.getMessage(), e1);
        }

        final Authentication authResult;
        Authentication authentication;

        try
        {
            authentication = new PGPAuthentication(userId, decodeMsgRaw(creds));
        }
        catch (Exception e1)
        {
            throw new UnauthorisedException(
                CoreMessages.failedToReadPayload(), event.getMessage(), e1);
        }

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

            throw new UnauthorisedException(CoreMessages.authFailedForUser(userId), event.getMessage(), e);
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
            updatePayload(event.getMessage(), getUnencryptedMessageWithoutSignature((PGPAuthentication)authResult));
//            TODO RequestContext.rewriteEvent(new DefaultMuleMessage(
//                getUnencryptedMessageWithoutSignature((PGPAuthentication)authResult)));
        }
        catch (Exception e2)
        {
            throw new UnauthorisedException(event.getMessage(), context, event.getEndpoint(), this);
        }
    }

    private Message decodeMsgRaw(byte[] raw) throws Exception
    {
        MessageFactory mf = MessageFactory.getInstance("OpenPGP");

        ByteArrayInputStream in = new ByteArrayInputStream(raw);

        Collection msgs = mf.generateMessages(in);

        return (Message)msgs.iterator().next();
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

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.security.AbstractEndpointSecurityFilter#authenticateOutbound(org.mule.api.MuleEvent)
     */
    protected void authenticateOutbound(MuleEvent event) throws SecurityException, UnauthorisedException
    {
        logger.debug("authenticateOutbound:" + event.getId());

        if (!isAuthenticate())
        {
            return;
        }

        MuleMessage message = event.getMessage();

        KeyBundle userKeyBundle = keyManager.getKeyBundle((String)getCredentialsAccessor().getCredentials(
            event));

        final PGPCryptInfo cryptInfo = new PGPCryptInfo(userKeyBundle, signRequired);

        try
        {
            updatePayload(event.getMessage(), strategy.encrypt(message.getPayloadAsBytes(), cryptInfo));
        }
        catch (Exception e1)
        {
            throw new UnauthorisedException(CoreMessages.failedToReadPayload(), event.getMessage(), e1);
        }
    }

    protected void doInitialise() throws InitialisationException
    {
        if (strategyName != null)
        {
            strategy = endpoint.getMuleContext().getSecurityManager().getEncryptionStrategy(strategyName);
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
