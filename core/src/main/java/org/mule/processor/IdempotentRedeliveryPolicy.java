/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.store.ObjectAlreadyExistsException;
import org.mule.api.store.ObjectStoreException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.simple.ByteArrayToHexString;
import org.mule.transformer.simple.SerializableToByteArray;
import org.mule.util.store.AbstractMonitoredObjectStore;
import org.mule.util.store.InMemoryObjectStore;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement a retry policy for Mule.  This is similar to JMS retry policies that will redeliver a message a maximum
 * number of times.  If this maximum is exceeded, the message is sent to a dead letter queue,  Here, if the processing of the messages
 * fails too often, the message is sent to the failedMessageProcessor MP, whence success is force to be returned, to allow
 * the message to be considered "consumed".
 */
public class IdempotentRedeliveryPolicy extends AbstractRedeliveryPolicy
{
    private final SerializableToByteArray objectToByteArray = new SerializableToByteArray();
    private final ByteArrayToHexString byteArrayToHexString = new ByteArrayToHexString();

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    private boolean useSecureHash;
    private String messageDigestAlgorithm;
    private String idExpression;
    private AbstractMonitoredObjectStore<AtomicInteger> store;

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        if (useSecureHash && idExpression != null)
        {
            throw new InitialisationException(
                CoreMessages.initialisationFailure(String.format(
                    "The Id expression'%s' was specified when a secure hash will be used",
                    idExpression)), this);
        }
        if (!useSecureHash && messageDigestAlgorithm != null)
        {
            throw new InitialisationException(
                CoreMessages.initialisationFailure(String.format(
                    "The message digest algorithm '%s' was specified when a secure hash will not be used",
                    messageDigestAlgorithm)), this);
        }
        if (!useSecureHash && idExpression == null)
        {
            throw new InitialisationException(
                CoreMessages.initialisationFailure(
                    "No method for identifying messages was specified"), this);
        }
        if (useSecureHash)
        {
            if (messageDigestAlgorithm == null)
            {
                messageDigestAlgorithm = "SHA-256";
            }
            try
            {
                MessageDigest md = MessageDigest.getInstance(messageDigestAlgorithm);
            }
            catch (NoSuchAlgorithmException e)
            {
                throw new InitialisationException(
                    CoreMessages.initialisationFailure(
                        String.format("Exception '%s' initializing message digest algorithm %s", e.getMessage(), messageDigestAlgorithm)), this);

            }
        }

        store = createStore();
    }

    private AbstractMonitoredObjectStore<AtomicInteger> createStore() throws InitialisationException
    {
        AbstractMonitoredObjectStore s = new InMemoryObjectStore<AtomicInteger>();
        s.setName(flowConstruct.getName() + "." + getClass().getName());
        s.setMaxEntries(-1);
        s.setEntryTTL(60 * 5 * 1000);
        s.setExpirationInterval(6000);
        s.initialise();
        return s;
    }


    @Override
    public void dispose()
    {
        super.dispose();

        if (store != null)
        {
            store.dispose();
            store = null;
        }

        if (deadLetterQueue instanceof Disposable)
        {
            ((Disposable) deadLetterQueue).dispose();
        }
    }

    @Override
    public void start() throws MuleException
    {
        if (deadLetterQueue instanceof Startable)
        {
            ((Startable) deadLetterQueue).start();
        }
    }


    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        boolean exceptionSeen = false;
        boolean tooMany = false;
        AtomicInteger counter = null;

        String messageId = null;
        try
        {
            messageId = getIdForEvent(event);
        }
        catch (Exception ex)
        {
            exceptionSeen = true;
        }

        if (!exceptionSeen)
        {
            counter = getCounter(messageId, null, false);
            tooMany = counter != null && counter.get() > maxRedeliveryCount;
        }

        if (tooMany || exceptionSeen)
        {
            try
            {
                return deadLetterQueue.process(event);
            }
            catch (Exception ex)
            {
                logger.info("Exception thrown from failed message processing for message " + messageId, ex);
            }
            return null;
        }

        try
        {
            MuleEvent returnEvent = processNext(event);
            counter = getCounter(messageId, counter, false);
            if (counter != null)
            {
                counter.set(0);
            }
            return returnEvent;
        }
        catch (MuleException ex)
        {
            incrementCounter(messageId, counter);
            throw ex;
        }
        catch (RuntimeException ex)
        {
            incrementCounter(messageId, counter);
            throw ex;
        }
    }


    private AtomicInteger incrementCounter(String messageId, AtomicInteger counter) throws ObjectStoreException
    {
        counter = getCounter(messageId,  counter, true);
        counter.incrementAndGet();
        return counter;
    }

    private AtomicInteger getCounter(String messageId, AtomicInteger counter, boolean create) throws ObjectStoreException
    {
        if (counter != null)
        {
            return counter;
        }
        boolean counterExists = store.contains(messageId);
        if (counterExists)
        {
            return store.retrieve(messageId);
        }
        if (create)
        {
            try
            {
                counter = new AtomicInteger();
                store.store(messageId, counter);
            }
            catch (ObjectAlreadyExistsException e)
            {
                counter = store.retrieve(messageId);
            }
        }
        return counter;
    }


    private String getIdForEvent(MuleEvent event) throws Exception
    {
        if (useSecureHash)
        {
            Object payload = event.getMessage().getPayload();
            byte[] bytes = (byte[]) objectToByteArray.transform(payload);
            MessageDigest md = MessageDigest.getInstance(messageDigestAlgorithm);
            byte[] digestedBytes = md.digest(bytes);
            return (String)byteArrayToHexString.transform(digestedBytes);
        }
        else
        {
             return event.getMuleContext().getExpressionManager().parse(idExpression, event.getMessage(), true);
        }
    }

    public boolean isUseSecureHash()
    {
        return useSecureHash;
    }

    public void setUseSecureHash(boolean useSecureHash)
    {
        this.useSecureHash = useSecureHash;
    }

    public String getMessageDigestAlgorithm()
    {
        return messageDigestAlgorithm;
    }

    public void setMessageDigestAlgorithm(String messageDigestAlgorithm)
    {
        this.messageDigestAlgorithm = messageDigestAlgorithm;
    }

    public String getIdExpression()
    {
        return idExpression;
    }

    public void setIdExpression(String idExpression)
    {
        this.idExpression = idExpression;
    }
}
