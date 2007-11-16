/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.MuleRuntimeException;
import org.mule.RegistryContext;
import org.mule.config.i18n.CoreMessages;
import org.mule.providers.AbstractMessageAdapter;
import org.mule.providers.DefaultMessageAdapter;
import org.mule.providers.NullPayload;
import org.mule.transformers.TransformerUtils;
import org.mule.umo.UMOExceptionPayload;
import org.mule.umo.UMOMessage;
import org.mule.umo.provider.PropertyScope;
import org.mule.umo.provider.UMOMessageAdapter;
import org.mule.umo.provider.UMOMutableMessageAdapter;
import org.mule.umo.transformer.TransformerException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.DebugOptions;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleMessage</code> is a wrapper that contains a payload and properties
 * associated with the payload.
 */

public class MuleMessage implements UMOMessage, ThreadSafeAccess
{
    /** Serial version */
    private static final long serialVersionUID = 1541720810851984842L;
    private static Log logger = LogFactory.getLog(MuleMessage.class);

    private UMOMessageAdapter adapter;
    private UMOMessageAdapter originalAdapter = null;
    private transient List appliedTransformerHashCodes = new CopyOnWriteArrayList();
    private byte[] cache;

    public MuleMessage(Object message)
    {
        this(message, (Map) null);
    }

    public MuleMessage(Object message, Map properties)
    {
        if (message instanceof UMOMessageAdapter)
        {
            adapter = (UMOMessageAdapter) message;
        }
        else
        {
            adapter = new DefaultMessageAdapter(message);
        }
        addProperties(properties);
        resetAccessControl();
    }


    public MuleMessage(Object message, UMOMessageAdapter previous)
    {
        if (message instanceof UMOMessageAdapter)
        {
            adapter = (UMOMessageAdapter) message;
            ((ThreadSafeAccess) adapter).resetAccessControl();
        }
        else
        {
            adapter = new DefaultMessageAdapter(message, previous);
        }
        if (previous.getExceptionPayload() != null)
        {
            setExceptionPayload(previous.getExceptionPayload());
        }
        setEncoding(previous.getEncoding());
        if (previous.getAttachmentNames().size() > 0)
        {
            Set attNames = adapter.getAttachmentNames();
            synchronized (attNames)
            {
                for (Iterator iterator = attNames.iterator(); iterator.hasNext();)
                {
                    String s = (String) iterator.next();
                    try
                    {
                        addAttachment(s, adapter.getAttachment(s));
                    }
                    catch (Exception e)
                    {
                        throw new MuleRuntimeException(CoreMessages.failedToReadAttachment(s), e);
                    }
                }
            }
        }
        resetAccessControl();
    }

    /** {@inheritDoc} */
    public Object getPayload(Class outputType) throws TransformerException
    {
        return getPayload(outputType, getEncoding());
    }

    /**
     * Will attempt to obtain the payload of this message with the desired Class type. This will
     * try and resolve a trnsformr that can do this transformation. If a transformer cannot be found
     * an exception is thrown.  Any transfromers added to the reqgistry will be checked for compatability
     *
     * @param outputType the desired return type
     * @param encoding   the encoding to use if required
     * @return The converted payload of this message. Note that this method will not alter the payload of this
     *         message *unless* the payload is an inputstream in which case the stream will be read and the payload will become
     *         the fully read stream.
     * @throws TransformerException if a transformer cannot be found or there is an error during transformation of the
     *                              payload
     */
    protected Object getPayload(Class outputType, String encoding) throws TransformerException
    {
        //Handle null by ignoring the request
        if (outputType == null)
        {
            return getPayload();
        }

        Class inputCls = getPayload().getClass();

        //Special case where proxies are used for testing
        if (Proxy.isProxyClass(inputCls))
        {
            inputCls = inputCls.getInterfaces()[0];
        }

        //If no conversion is necessary, just return the payload as-is
        if (outputType.isAssignableFrom(inputCls))
        {
            return getPayload();
        }
        //Grab a list of transformers that batch out input/output requirements
        // List transformers = RegistryContext.getRegistry().lookupTransformers(inputCls, outputType);

        //The transformer to execute on this message
        UMOTransformer transformer = null;
        transformer = RegistryContext.getRegistry().lookupTransformer(inputCls, outputType);

        //no transformers found
        if (transformer == null)
        {
            throw new TransformerException(CoreMessages.noTransformerFoundForMessage(inputCls, outputType));
        }

        // Pass in the adapter itself, so we respect the encoding
        Object result = transformer.transform(this);

        //TODO Unless we disallow Object.class as a valid return type we need to do this extra check
        if (!outputType.isAssignableFrom(result.getClass()))
        {
            throw new TransformerException(CoreMessages.transformOnObjectNotOfSpecifiedType(outputType.getName(), result.getClass()));
        }
        //If the payload is a stream and we've consumed it, then we should
        //set the payload on the message
        //This is the only time this method will alter the payload on the message
        if (isPayloadConsumed(inputCls))
        {
            setPayload(result);
        }

        return result;
    }

    /**
     * Checks if the payload has been consumed for this message. This only applies to Streaming payload types
     * since once the stream has been read, the payload of the message should be updated to represent the data read
     * from the stream
     *
     * @param inputCls the input type of the message payload
     * @return true if the payload message type was stream-based, false otherwise
     */
    protected boolean isPayloadConsumed(Class inputCls)
    {
        return InputStream.class.isAssignableFrom(inputCls);
    }

    /** {@inheritDoc} */
    public UMOMessageAdapter getAdapter()
    {
        return adapter;
    }

    /** {@inheritDoc} */
    public Object getOrginalPayload()
    {
        return (originalAdapter == null ? adapter.getPayload() : originalAdapter.getPayload());
    }

    /** {@inheritDoc} */
    public UMOMessageAdapter getOriginalAdapter()
    {
        return (originalAdapter == null ? adapter : originalAdapter);
    }

    /** {@inheritDoc} */
    public void setProperty(String key, Object value, PropertyScope scope)
    {
        adapter.setProperty(key, value, scope);
    }


    /** {@inheritDoc} */
    public Object getProperty(String key)
    {
        return adapter.getProperty(key);
    }

    /** {@inheritDoc} */
    public Object removeProperty(String key)
    {
        return adapter.removeProperty(key);
    }

    /** {@inheritDoc} */
    public void setProperty(String key, Object value)
    {
        adapter.setProperty(key, value);
    }

    /** {@inheritDoc} */
    public final String getPayloadAsString() throws Exception
    {
        assertAccess(READ);
        return getPayloadAsString(getEncoding());
    }

    /** {@inheritDoc} */
    public byte[] getPayloadAsBytes() throws Exception
    {
        assertAccess(READ);
        if (cache != null)
        {
            return cache;
        }
        byte[] result = (byte[]) getPayload(byte[].class);
        if (DebugOptions.isCacheMessageAsBytes())
        {
            cache = result;
        }
        return result;
    }

    /** {@inheritDoc} */
    public String getPayloadAsString(String encoding) throws Exception
    {
        assertAccess(READ);
        if (cache != null)
        {
            return new String(cache, encoding);
        }
        String result = (String) getPayload(String.class);
        if (DebugOptions.isCacheMessageAsBytes())
        {
            cache = result.getBytes(encoding);
        }
        return result;
    }

    /** {@inheritDoc} */
    public Set getPropertyNames()
    {
        return adapter.getPropertyNames();
    }

    //** {@inheritDoc} */
    public double getDoubleProperty(String name, double defaultValue)
    {
        return adapter.getDoubleProperty(name, defaultValue);
    }

    /** {@inheritDoc} */
    public void setDoubleProperty(String name, double value)
    {
        adapter.setDoubleProperty(name, value);
    }

    /** {@inheritDoc} */
    public String getUniqueId()
    {
        return adapter.getUniqueId();
    }

    /** {@inheritDoc} */
    public Object getProperty(String name, Object defaultValue)
    {
        return adapter.getProperty(name, defaultValue);
    }

    /** {@inheritDoc} */
    public int getIntProperty(String name, int defaultValue)
    {
        return adapter.getIntProperty(name, defaultValue);
    }

    /** {@inheritDoc} */
    public long getLongProperty(String name, long defaultValue)
    {
        return adapter.getLongProperty(name, defaultValue);
    }

    /** {@inheritDoc} */
    public boolean getBooleanProperty(String name, boolean defaultValue)
    {
        return adapter.getBooleanProperty(name, defaultValue);
    }

    /** {@inheritDoc} */
    public void setBooleanProperty(String name, boolean value)
    {
        adapter.setBooleanProperty(name, value);
    }

    /** {@inheritDoc} */
    public void setIntProperty(String name, int value)
    {
        adapter.setIntProperty(name, value);
    }

    /** {@inheritDoc} */
    public void setLongProperty(String name, long value)
    {
        adapter.setLongProperty(name, value);
    }

    /** {@inheritDoc} */
    public void setCorrelationId(String id)
    {
        adapter.setCorrelationId(id);
    }

    /** {@inheritDoc} */
    public String getCorrelationId()
    {
        return adapter.getCorrelationId();
    }

    /** {@inheritDoc} */
    public void setReplyTo(Object replyTo)
    {
        adapter.setReplyTo(replyTo);
    }

    /** {@inheritDoc} */
    public Object getReplyTo()
    {
        return adapter.getReplyTo();
    }

    /** {@inheritDoc} */
    public int getCorrelationSequence()
    {
        return adapter.getCorrelationSequence();
    }

    /** {@inheritDoc} */
    public void setCorrelationSequence(int sequence)
    {
        adapter.setCorrelationSequence(sequence);
    }

    /** {@inheritDoc} */
    public int getCorrelationGroupSize()
    {
        return adapter.getCorrelationGroupSize();
    }

    //** {@inheritDoc} */
    public void setCorrelationGroupSize(int size)
    {
        adapter.setCorrelationGroupSize(size);
    }

    /** {@inheritDoc} */
    public UMOExceptionPayload getExceptionPayload()
    {
        return adapter.getExceptionPayload();
    }

    /** {@inheritDoc} */
    public void setExceptionPayload(UMOExceptionPayload exceptionPayload)
    {
        adapter.setExceptionPayload(exceptionPayload);
    }

    /** {@inheritDoc} */
    public String toString()
    {
        return adapter.toString();
    }

    /** {@inheritDoc} */
    public void addAttachment(String name, DataHandler dataHandler) throws Exception
    {
        adapter.addAttachment(name, dataHandler);
    }

    /** {@inheritDoc} */
    public void removeAttachment(String name) throws Exception
    {
        adapter.removeAttachment(name);
    }

    /** {@inheritDoc} */
    public DataHandler getAttachment(String name)
    {
        return adapter.getAttachment(name);
    }

    /** {@inheritDoc} */
    public Set getAttachmentNames()
    {
        return adapter.getAttachmentNames();
    }

    /** {@inheritDoc} */
    public String getEncoding()
    {
        return adapter.getEncoding();
    }

    /** {@inheritDoc} */
    public void setEncoding(String encoding)
    {
        adapter.setEncoding(encoding);
    }

    /** {@inheritDoc} */
    public String getStringProperty(String name, String defaultValue)
    {
        return adapter.getStringProperty(name, defaultValue);
    }

    /** {@inheritDoc} */
    public void setStringProperty(String name, String value)
    {
        adapter.setStringProperty(name, value);
    }


    /** {@inheritDoc} */
    public void addProperties(Map properties)
    {
        adapter.addProperties(properties);
    }

    /** {@inheritDoc} */
    public void clearProperties()
    {
        adapter.clearProperties();
    }

    /** {@inheritDoc} */
    public Object getPayload()
    {
        return adapter.getPayload();
    }

    /** {@inheritDoc} */
    public synchronized void setPayload(Object payload)
    {
        //TODO we may want to enforce stricter rules here, rather than silently wrapping the existing adapter
        if (!(adapter instanceof UMOMutableMessageAdapter))
        {
            adapter = new DefaultMessageAdapter(payload, adapter);
        }
        else
        {
            ((UMOMutableMessageAdapter) adapter).setPayload(payload);
        }
        cache = null;
    }

    /** {@inheritDoc} */
    public void release()
    {
        adapter.release();
        if (originalAdapter != null)
        {
            originalAdapter.release();
        }
        cache = null;
    }

    /** {@inheritDoc} */
    public void applyTransformers(List transformers) throws TransformerException
    {
        applyTransformers(transformers, null);
    }

    public void applyTransformers(List transformers, Class outputType) throws TransformerException
    {
        if (TransformerUtils.isDefined(transformers) && transformers.size() > 0 &&
                !appliedTransformerHashCodes.contains(new Integer(transformers.hashCode())))
        {
            applyAllTransformers(transformers);
            appliedTransformerHashCodes.add(new Integer(transformers.hashCode()));
        }

        if (null != outputType && !getPayload().getClass().isAssignableFrom(outputType))
        {
            setPayload(getPayload(outputType));
        }
    }

    protected void applyAllTransformers(List transformers) throws TransformerException
    {
        if (TransformerUtils.isDefined(transformers) && transformers.size() > 0)
        {

            Iterator iterator = transformers.iterator();
            while (iterator.hasNext())
            {
                UMOTransformer transformer = (UMOTransformer) iterator.next();

                if (getPayload() == null)
                {
                    if (transformer.isAcceptNull())
                    {
                        setPayload(NullPayload.getInstance());
                    }
                    else
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Transformer " + transformer +
                                    " doesn't support the null payload, exiting from transformer chain.");
                        }
                        break;
                    }
                }

                Class srcCls = getPayload().getClass();
                if (transformer.isSourceTypeSupported(srcCls))
                {
                    Object result = transformer.transform(this);

                    if (originalAdapter == null && DebugOptions.isCacheMessageOriginalPayload())
                    {
                        originalAdapter = adapter;
                    }
                    //TODO RM*: Must make sure this works for all scenarios
                    if (result instanceof UMOMessage)
                    {
                        synchronized (this)
                        {
                            adapter = ((UMOMessage) result).getAdapter();
                        }
                    }
                    else
                    {
                        setPayload(result);
                    }
                }
                else
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Transformer " + transformer + " doesn't support the source payload: " + srcCls);
                    }
                    if (!transformer.isIgnoreBadInput())
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Exiting from transformer chain (ignoreBadInput = false)");
                        }
                        break;
                    }
                }
            }
        }
    }

    //////////////////////////////// ThreadSafeAccess Impl ///////////////////////////////

    /** {@inheritDoc} */
    public ThreadSafeAccess newThreadCopy()
    {
        if (adapter instanceof ThreadSafeAccess)
        {
            logger.debug("new copy of message for " + Thread.currentThread());
            return new MuleMessage(((ThreadSafeAccess) adapter).newThreadCopy(), this);
        }
        else
        {
            // not much we can do here - streaming will have to handle things itself
            return this;
        }
    }

    /** {@inheritDoc} */
    public void resetAccessControl()
    {
        if (adapter instanceof AbstractMessageAdapter)
        {
            ((AbstractMessageAdapter) adapter).resetAccessControl();
        }
    }

    /** {@inheritDoc} */
    public void assertAccess(boolean write)
    {
        if (adapter instanceof AbstractMessageAdapter)
        {
            ((AbstractMessageAdapter) adapter).assertAccess(write);
        }
    }

    protected void finalize() throws Throwable
    {
        release();
    }
}
