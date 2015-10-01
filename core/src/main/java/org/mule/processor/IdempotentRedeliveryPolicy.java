/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.processor;

import static org.mule.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.exception.MessageRedeliveredException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.ObjectStoreManager;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.simple.ByteArrayToHexString;
import org.mule.transformer.simple.ObjectToByteArray;
import org.mule.util.lock.LockFactory;
import org.mule.util.store.ObjectStorePartition;

import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

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
    private final ObjectToByteArray objectToByteArray = new ObjectToByteArray();
    private final ByteArrayToHexString byteArrayToHexString = new ByteArrayToHexString();

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    private boolean useSecureHash;
    private String messageDigestAlgorithm;
    private String idExpression;
    private ObjectStore<AtomicInteger> store;
    private LockFactory lockFactory;
    private String idrId;

    @Override
    public void initialise() throws InitialisationException
    {
        super.initialise();
        if (useSecureHash && idExpression != null)
        {
            useSecureHash = false;
            if (logger.isWarnEnabled())
            {
                logger.warn("Disabling useSecureHash in idempotent-redelivery-policy since an idExpression has been configured");
            }
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
                MessageDigest.getInstance(messageDigestAlgorithm);
            }
            catch (NoSuchAlgorithmException e)
            {
                throw new InitialisationException(
                    CoreMessages.initialisationFailure(
                        String.format("Exception '%s' initializing message digest algorithm %s", e.getMessage(), messageDigestAlgorithm)), this);

            }
        }

        String appName = muleContext.getConfiguration().getId();
        String flowName = flowConstruct.getName();
        idrId = String.format("%s-%s-%s",appName,flowName,"idr");
        lockFactory = muleContext.getLockFactory();
        store = createStore();
        initialiseIfNeeded(objectToByteArray, muleContext);
        initialiseIfNeeded(byteArrayToHexString, muleContext);
    }

    private ObjectStore<AtomicInteger> createStore() throws InitialisationException
    {
        ObjectStoreManager objectStoreManager = (ObjectStoreManager) muleContext.getRegistry().get(
                                MuleProperties.OBJECT_STORE_MANAGER);
        return objectStoreManager.getObjectStore(flowConstruct.getName() + "." + getClass().getName(), false, -1,  60 * 5 * 1000, 6000 );
    }


    @Override
    public void dispose()
    {
        super.dispose();

        if (store != null)
        {
            if (store instanceof ObjectStorePartition)
            {
                try
                {
                    ((ObjectStorePartition)store).close();
                }
                catch (ObjectStoreException e)
                {
                    logger.warn("error closing object store: " + e.getMessage(), e);
                }
            }
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
        catch (TransformerException e)
        {
            logger.warn("The message cannot be processed because the digest could not be generated. Either make the payload serializable or use an expression.");
            return null;
        }
        catch (Exception ex)
        {
            exceptionSeen = true;
        }

        Lock lock = lockFactory.createLock(idrId + "-" + messageId);
        lock.lock();
        try
        {

            if (!exceptionSeen)
            {
                counter = findCounter(messageId);
                tooMany = counter != null && counter.get() > maxRedeliveryCount;
            }

            if (tooMany || exceptionSeen)
            {
                try
                {
                    if (deadLetterQueue != null)
                    {
                        return deadLetterQueue.process(event);
                    }
                    else
                    {
                        throw new MessageRedeliveredException(messageId, counter.get(), maxRedeliveryCount, null, event, this);
                    }
                }
                catch (MessageRedeliveredException ex)
                {
                    throw ex;
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
                counter = findCounter(messageId);
                if (counter != null)
                {
                    resetCounter(messageId);
                }
                return returnEvent;
            }
            catch (MuleException ex)
            {
                incrementCounter(messageId);
                throw ex;
            }
            catch (RuntimeException ex)
            {
                incrementCounter(messageId);
                throw ex;
            }
        }
        finally
        {
            lock.unlock();
        }

    }

    private void resetCounter(String messageId) throws ObjectStoreException
    {
        store.remove(messageId);
        store.store(messageId, new AtomicInteger());
    }

    public AtomicInteger findCounter(String messageId) throws ObjectStoreException
    {
        boolean counterExists = store.contains(messageId);
        if (counterExists)
        {
            return store.retrieve(messageId);
        }
        return null;
    }

    private AtomicInteger incrementCounter(String messageId) throws ObjectStoreException
    {
        AtomicInteger counter = findCounter(messageId);
        if (counter == null)
        {
            counter = new AtomicInteger();
        }
        else
        {
            store.remove(messageId);
        }
        counter.incrementAndGet();
        store.store(messageId,counter);
        return counter;
    }

    private String getIdForEvent(MuleEvent event) throws Exception
    {
        if (useSecureHash)
        {
            Object payload = event.getMessage().getPayload();
            byte[] bytes = (byte[]) objectToByteArray.transform(payload);
            if (payload instanceof InputStream)
            {
                // We've consumed the stream.
                event.getMessage().setPayload(bytes);
            }
            MessageDigest md = MessageDigest.getInstance(messageDigestAlgorithm);
            byte[] digestedBytes = md.digest(bytes);
            return (String)byteArrayToHexString.transform(digestedBytes);
        }
        else
        {
             return event.getMuleContext().getExpressionManager().parse(idExpression, event, true);
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
    
    public void setMessageProcessor(MessageProcessor processor)
    {
        this.deadLetterQueue = processor;
    }
}

