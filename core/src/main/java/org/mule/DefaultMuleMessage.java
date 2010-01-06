/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleRuntimeException;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.MessageAdapter;
import org.mule.api.transport.MutableMessageAdapter;
import org.mule.api.transport.OutputHandler;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.SimpleDataType;
import org.mule.transport.AbstractMessageAdapter;
import org.mule.transport.DefaultMessageAdapter;
import org.mule.transport.MessageAdapterSerialization;
import org.mule.transport.NullPayload;
import org.mule.util.ClassUtils;
import org.mule.util.store.DeserializationPostInitialisable;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>DefaultMuleMessage</code> is a wrapper that contains a payload and properties
 * associated with the payload.
 */

public class DefaultMuleMessage implements MuleMessage, ThreadSafeAccess, DeserializationPostInitialisable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 1541720810851984842L;
    private static Log logger = LogFactory.getLog(DefaultMuleMessage.class);

    private transient MessageAdapter adapter;
    private transient MessageAdapter originalAdapter = null;
    private transient List<Integer> appliedTransformerHashCodes;
    private transient byte[] cache;
    protected transient MuleContext muleContext;

    private static final List<Class<?>> consumableClasses = new ArrayList<Class<?>>();

    static
    {
        try
        {
            consumableClasses.add(ClassUtils.loadClass("javax.xml.stream.XMLStreamReader",
                    DefaultMuleMessage.class));
        }
        catch (ClassNotFoundException e)
        {
            // ignore
        }

        try
        {
            consumableClasses.add(ClassUtils.loadClass("javax.xml.transform.stream.StreamSource",
                    DefaultMuleMessage.class));
        }
        catch (ClassNotFoundException e)
        {
            // ignore
        }

        consumableClasses.add(OutputHandler.class);
    }

    public DefaultMuleMessage(Object message, MuleContext muleContext)
    {
        this(message, (Map) null, muleContext);
    }

    public DefaultMuleMessage(Object message, Map properties, MuleContext muleContext)
    {
        if (muleContext == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("muleContext").getMessage());
        }
        this.muleContext = muleContext;

        initAppliedTransformerHashCodes();

        // Explicitly check for MuleMessage as a safeguard since MuleMessage is instance of MessageAdapter
        if (message instanceof MuleMessage)
        {
            adapter = ((MuleMessage) message).getAdapter();
        }
        else if (message instanceof MessageAdapter)
        {
            adapter = (MessageAdapter) message;
        }
        else
        {
            adapter = new DefaultMessageAdapter(message);
        }
        addProperties(properties);
        resetAccessControl();
    }


    public DefaultMuleMessage(Object message, MessageAdapter previous, MuleContext muleContext)
    {
        if (muleContext == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("muleContext").getMessage());
        }
        this.muleContext = muleContext;

        initAppliedTransformerHashCodes();

        if (message instanceof MessageAdapter)
        {
            adapter = (MessageAdapter) message;
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
            Set<String> attNames = adapter.getAttachmentNames();
            for (String s : attNames)
            {
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
        resetAccessControl();
    }

    protected DefaultMuleMessage(DefaultMuleMessage message)
    {
        this(message.getPayload(), message.getAdapter(), message.getMuleContext());
    }

    @SuppressWarnings("unchecked")
    private void initAppliedTransformerHashCodes()
    {
        appliedTransformerHashCodes = new CopyOnWriteArrayList();
    }

    /**
     * {@inheritDoc}
     */
    public <T> T getPayload(Class<T> outputType) throws TransformerException
    {
        return getPayload(outputType, getEncoding());
    }

    MuleContext getMuleContext()
    {
        return muleContext;
    }

    /**
     * Will attempt to obtain the payload of this message with the desired Class type. This will
     * try and resolve a transformer that can do this transformation. If a transformer cannot be 
     * found an exception is thrown. Any transformers added to the registry will be checked for 
     * compatability.
     *
     * @param outputType the desired return type
     * @param encoding the encoding to use if required
     * @return The converted payload of this message. Note that this method will not alter the 
     *          payload of this message <b>unless</b> the payload is an {@link InputStream} in which
     *          case the stream will be read and the payload will become the fully read stream.
     * @throws TransformerException if a transformer cannot be found or there is an error during 
     *          transformation of the payload.
     * @since 3.0.0
     */
    @SuppressWarnings("unchecked")
    protected <T> T getPayload(DataType<T> resultType, String encoding) throws TransformerException
    {
        // Handle null by ignoring the request
        if (resultType == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("resultType").getMessage());
        }

        //TODO handling of mime type
        DataType<?> source = new DataTypeFactory().create(getPayload().getClass());

        // If no conversion is necessary, just return the payload as-is
        if (resultType.isCompatibleWith(source))
        {
            return (T) getPayload();
        }

        // The transformer to execute on this message
        Transformer transformer = muleContext.getRegistry().lookupTransformer(source, resultType);
        if (transformer == null)
        {
            throw new TransformerException(CoreMessages.noTransformerFoundForMessage(source, resultType));
        }
        
        // Pass in the adapter itself
        Object result = transformer.transform(this, encoding);

        // Unless we disallow Object.class as a valid return type we need to do this extra check
        if (!resultType.getType().isAssignableFrom(result.getClass()))
        {
            throw new TransformerException(CoreMessages.transformOnObjectNotOfSpecifiedType(resultType, result));
        }
        
        // If the payload is a stream and we've consumed it, then we should set the payload on the 
        // message. This is the only time this method will alter the payload on the message
        if (isPayloadConsumed(source.getType()))
        {
            setPayload(result);
        }

        return (T) result;
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
    protected <T> T getPayload(Class<T> outputType, String encoding) throws TransformerException
    {
        return (T) getPayload(new SimpleDataType(outputType), encoding);
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
        return InputStream.class.isAssignableFrom(inputCls) || isConsumedFromAdditional(inputCls);
    }

    private boolean isConsumedFromAdditional(Class<?> inputCls)
    {
        if (consumableClasses.isEmpty())
        {
            return false;
        }

        for (Class<?> c : consumableClasses)
        {
            if (c.isAssignableFrom(inputCls))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public MessageAdapter getAdapter()
    {
        return adapter;
    }

    /**
     * {@inheritDoc}
     */
    public Object getOrginalPayload()
    {
        return (originalAdapter == null ? adapter.getPayload() : originalAdapter.getPayload());
    }

    /**
     * {@inheritDoc}
     */
    public MessageAdapter getOriginalAdapter()
    {
        return (originalAdapter == null ? adapter : originalAdapter);
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty(String key, Object value, PropertyScope scope)
    {
        adapter.setProperty(key, value, scope);
    }

    /**
     * {@inheritDoc}
     */
    public Object getProperty(String key)
    {
        return adapter.getProperty(key);
    }

    /**
     * {@inheritDoc}
     */
    public Object removeProperty(String key)
    {
        return adapter.removeProperty(key);
    }

    /**
     * {@inheritDoc}
     */
    public Object removeProperty(String key, PropertyScope scope)
    {
        return adapter.removeProperty(key, scope);
    }

    /** {@inheritDoc} */
    public void setProperty(String key, Object value)
    {
        adapter.setProperty(key, value);
    }

    /**
     * {@inheritDoc}
     */
    public final String getPayloadAsString() throws Exception
    {
        assertAccess(READ);
        return getPayloadAsString(getEncoding());
    }

    /**
     * {@inheritDoc}
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        assertAccess(READ);
        if (cache != null)
        {
            return cache;
        }
        byte[] result = (byte[]) getPayload(byte[].class);
        if (muleContext.getConfiguration().isCacheMessageAsBytes())
        {
            cache = result;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public String getPayloadAsString(String encoding) throws Exception
    {
        assertAccess(READ);
        if (cache != null)
        {
            return new String(cache, encoding);
        }
        String result = (String) getPayload(String.class, encoding);
        if (muleContext.getConfiguration().isCacheMessageAsBytes())
        {
            cache = result.getBytes(encoding);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getPropertyNames()
    {
        return adapter.getPropertyNames();
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getPropertyNames(PropertyScope scope)
    {
        return adapter.getPropertyNames(scope);
    }

    //** {@inheritDoc} */
    public double getDoubleProperty(String name, double defaultValue)
    {
        return adapter.getDoubleProperty(name, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    public void setDoubleProperty(String name, double value)
    {
        adapter.setDoubleProperty(name, value);
    }

    /**
     * {@inheritDoc}
     */
    public String getUniqueId()
    {
        return adapter.getUniqueId();
    }

    /**
     * {@inheritDoc}
     */
    public Object getProperty(String name, Object defaultValue)
    {
        return adapter.getProperty(name, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    public Object getProperty(String name, PropertyScope scope)
    {
        return adapter.getProperty(name, scope);
    }

    /**
     * {@inheritDoc}
     */
    public int getIntProperty(String name, int defaultValue)
    {
        return adapter.getIntProperty(name, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    public long getLongProperty(String name, long defaultValue)
    {
        return adapter.getLongProperty(name, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    public boolean getBooleanProperty(String name, boolean defaultValue)
    {
        return adapter.getBooleanProperty(name, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    public void setBooleanProperty(String name, boolean value)
    {
        adapter.setBooleanProperty(name, value);
    }

    /**
     * {@inheritDoc}
     */
    public void setIntProperty(String name, int value)
    {
        adapter.setIntProperty(name, value);
    }

    /**
     * {@inheritDoc}
     */
    public void setLongProperty(String name, long value)
    {
        adapter.setLongProperty(name, value);
    }

    /**
     * {@inheritDoc}
     */
    public void setCorrelationId(String id)
    {
        adapter.setCorrelationId(id);
    }

    /**
     * {@inheritDoc}
     */
    public String getCorrelationId()
    {
        return adapter.getCorrelationId();
    }

    /**
     * {@inheritDoc}
     */
    public void setReplyTo(Object replyTo)
    {
        adapter.setReplyTo(replyTo);
    }

    /**
     * {@inheritDoc}
     */
    public Object getReplyTo()
    {
        return adapter.getReplyTo();
    }

    /**
     * {@inheritDoc}
     */
    public int getCorrelationSequence()
    {
        return adapter.getCorrelationSequence();
    }

    /**
     * {@inheritDoc}
     */
    public void setCorrelationSequence(int sequence)
    {
        adapter.setCorrelationSequence(sequence);
    }

    /**
     * {@inheritDoc}
     */
    public int getCorrelationGroupSize()
    {
        return adapter.getCorrelationGroupSize();
    }

    //** {@inheritDoc} */
    public void setCorrelationGroupSize(int size)
    {
        adapter.setCorrelationGroupSize(size);
    }

    /**
     * {@inheritDoc}
     */
    public ExceptionPayload getExceptionPayload()
    {
        return adapter.getExceptionPayload();
    }

    /**
     * {@inheritDoc}
     */
    public void setExceptionPayload(ExceptionPayload exceptionPayload)
    {
        adapter.setExceptionPayload(exceptionPayload);
    }

    @Override
    public String toString()
    {
        return adapter.toString();
    }

    /**
     * {@inheritDoc}
     */
    public void addAttachment(String name, DataHandler dataHandler) throws Exception
    {
        adapter.addAttachment(name, dataHandler);
    }

    /**
     * {@inheritDoc}
     */
    public void removeAttachment(String name) throws Exception
    {
        adapter.removeAttachment(name);
    }

    /**
     * {@inheritDoc}
     */
    public DataHandler getAttachment(String name)
    {
        return adapter.getAttachment(name);
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getAttachmentNames()
    {
        return adapter.getAttachmentNames();
    }

    /**
     * {@inheritDoc}
     */
    public String getEncoding()
    {
        return adapter.getEncoding();
    }

    /**
     * {@inheritDoc}
     */
    public void setEncoding(String encoding)
    {
        adapter.setEncoding(encoding);
    }

    /**
     * {@inheritDoc}
     */
    public String getStringProperty(String name, String defaultValue)
    {
        return adapter.getStringProperty(name, defaultValue);
    }

    /**
     * {@inheritDoc}
     */
    public void setStringProperty(String name, String value)
    {
        adapter.setStringProperty(name, value);
    }

    /**
     * {@inheritDoc}
     */
    public void addProperties(Map<String, Object> properties)
    {
        adapter.addProperties(properties);
    }

    /**
     * {@inheritDoc}
     */
    public void addProperties(Map<String, Object> properties, PropertyScope scope)
    {
        adapter.addProperties(properties, scope);
    }

    /**
     * {@inheritDoc}
     */
    public void clearProperties()
    {
        adapter.clearProperties();
    }

    /**
     * {@inheritDoc}
     */
    public Object getPayload()
    {
        return adapter.getPayload();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void setPayload(Object payload)
    {
        //TODO we may want to enforce stricter rules here, rather than silently wrapping the existing adapter
        if (!(adapter instanceof MutableMessageAdapter))
        {
            adapter = new DefaultMessageAdapter(payload, adapter);
        }
        else
        {
            ((MutableMessageAdapter) adapter).setPayload(payload);
        }
        cache = null;
    }

    /**
     * {@inheritDoc}
     */
    public void release()
    {
        adapter.release();
        if (originalAdapter != null)
        {
            originalAdapter.release();
        }
        cache = null;
        appliedTransformerHashCodes.clear();
    }

    /**
     * {@inheritDoc}
     */
    public void applyTransformers(List<? extends Transformer> transformers) throws TransformerException
    {
        applyTransformers(transformers, null);
    }

    /**
     * {@inheritDoc}
     */
    public void applyTransformers(Transformer... transformers) throws TransformerException
    {
        applyTransformers(Arrays.asList(transformers), null);
    }

    public void applyTransformers(List<? extends Transformer> transformers, Class outputType) throws TransformerException
    {
        if (!transformers.isEmpty() && !appliedTransformerHashCodes.contains(transformers.hashCode()))
        {
            applyAllTransformers(transformers);
            appliedTransformerHashCodes.add(transformers.hashCode());
        }

        if (null != outputType && !getPayload().getClass().isAssignableFrom(outputType))
        {
            setPayload(getPayload(outputType));
        }
    }

    protected void applyAllTransformers(List transformers) throws TransformerException
    {
        if (!transformers.isEmpty())
        {

            for (Object transformer1 : transformers)
            {
                Transformer transformer = (Transformer) transformer1;

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

                    if (originalAdapter == null && muleContext.getConfiguration().isCacheMessageOriginalPayload())
                    {
                        originalAdapter = adapter;
                    }

                    if (result instanceof MuleMessage)
                    {
                        synchronized (this)
                        {
                            adapter = ((MuleMessage) result).getAdapter();
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

    /**
     * {@inheritDoc}
     */
    public ThreadSafeAccess newThreadCopy()
    {
        if (adapter instanceof ThreadSafeAccess)
        {
            logger.debug("new copy of message for " + Thread.currentThread());
            return new DefaultMuleMessage(((ThreadSafeAccess) adapter).newThreadCopy(), this, muleContext);
        }
        else
        {
            // not much we can do here - streaming will have to handle things itself
            return this;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void resetAccessControl()
    {
        if (adapter instanceof AbstractMessageAdapter)
        {
            ((AbstractMessageAdapter) adapter).resetAccessControl();
        }
        if (originalAdapter instanceof AbstractMessageAdapter)
        {
            ((AbstractMessageAdapter) originalAdapter).resetAccessControl();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void assertAccess(boolean write)
    {
        if (adapter instanceof AbstractMessageAdapter)
        {
            ((AbstractMessageAdapter) adapter).assertAccess(write);
        }
    }

    /**
     * Determines if the payload of this message is consumable i.e. it can't be read
     * more than once. This is here temporarily without adding to MuleMessage
     * interface until MULE-4256 is implemented.
     */
    public boolean isConsumable()
    {
        return isConsumedFromAdditional(this.getPayload().getClass());
    }

    private void writeObject(ObjectOutputStream out) throws Exception
    {
        out.defaultWriteObject();
        marshalMessageAdapter(out);
    }

    private void marshalMessageAdapter(ObjectOutputStream out) throws Exception
    {
        if (adapter instanceof MessageAdapterSerialization)
        {
            customMessageAdapterMarshalling(out);
        }
        else
        {
            defaultMessageAdapterMarshalling(out);
        }
    }

    private void customMessageAdapterMarshalling(ObjectOutputStream out) throws Exception
    {
        out.writeObject(MessageAdapterSerialization.Type.CustomSerialization);

        byte[] payload = ((MessageAdapterSerialization) adapter).getPayloadForSerialization();
        out.writeInt(payload.length);
        out.write(payload);

        marshalMessageAdapterProperties(out);
        marshalMessageAdapterAttachments(out);
    }

    private void defaultMessageAdapterMarshalling(ObjectOutputStream out) throws Exception
    {
        out.writeObject(MessageAdapterSerialization.Type.DefaultSerialization);
        out.writeObject(adapter);
    }

    private void marshalMessageAdapterProperties(ObjectOutputStream out) throws IOException
    {
        Set<String> propertyNames = adapter.getPropertyNames();
        out.writeInt(propertyNames.size());
        for (Object property : propertyNames)
        {
            String key = property.toString();
            out.writeObject(key);
            out.writeObject(getProperty(key));
        }
    }

    private void marshalMessageAdapterAttachments(ObjectOutputStream out)
    {
        // TODO Auto-generated method stub
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        in.defaultReadObject();
        unmarshalMessageAdapter(in);
    }

    private void unmarshalMessageAdapter(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        MessageAdapterSerialization.Type type = (MessageAdapterSerialization.Type) in.readObject();
        if (type == MessageAdapterSerialization.Type.DefaultSerialization)
        {
            defaultMessageAdapterUnmarshalling(in);
        }
        else
        {
            adapter = customMessageAdapterUnmarshalling(in);
            unmarshalMessageAdapterProperties(in, adapter);
            unmarshalMessageAdapterAttachments(in, adapter);
        }
    }

    private MessageAdapter customMessageAdapterUnmarshalling(ObjectInputStream in) throws IOException
    {
        int payloadSize = in.readInt();

        byte[] payload = new byte[payloadSize];
        in.read(payload);

        return new DefaultMessageAdapter(payload);
    }

    private void defaultMessageAdapterUnmarshalling(ObjectInputStream in) throws IOException, ClassNotFoundException
    {
        adapter = (MessageAdapter) in.readObject();
    }

    private void unmarshalMessageAdapterProperties(ObjectInputStream in, MessageAdapter messageAdapter)
            throws IOException, ClassNotFoundException
    {
        int propertyCount = in.readInt();
        for (int i = 0; i < propertyCount; i++)
        {
            String key = (String) in.readObject();
            Object value = in.readObject();
            messageAdapter.setProperty(key, value);
        }
    }

    private void unmarshalMessageAdapterAttachments(ObjectInputStream in, MessageAdapter messageAdapter)
            throws IOException, ClassNotFoundException
    {
        // TODO implement me
    }

    public static MuleMessage copy(MuleMessage message)
    {
        if (message instanceof DefaultMuleMessage)
        {
            return new DefaultMuleMessage((DefaultMuleMessage) message);
        }
        else
        {
            //Very unlikely to happen unless a user des something odd
            throw new IllegalArgumentException("In order to clone a message it must be assignable from: " + DefaultMuleMessage.class.getName());
        }
    }

    /**
     * Invoked after deserialization. This is called when the marker interface
     * {@link org.mule.util.store.DeserializationPostInitialisable} is used. This will get invoked
     * after the object has been deserialized passing in the current mulecontext when using either
     * {@link org.mule.transformer.wire.SerializationWireFormat},
     * {@link org.mule.transformer.wire.SerializedMuleMessageWireFormat} or the
     * {@link org.mule.transformer.simple.ByteArrayToSerializable} transformer.
     *
     * @param muleContext the current muleContext instance
     * @throws MuleException if there is an error initializing
     */
    void initAfterDeserialisation(MuleContext muleContext) throws MuleException
    {
        this.muleContext = muleContext;
    }

}
