/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer;

import org.mule.AbstractAnnotatedObject;
import org.mule.DefaultMessageCollection;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transformer.TransformerMessagingException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.Message;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.SimpleDataType;
import org.mule.transport.NullPayload;
import org.mule.util.ClassUtils;
import org.mule.util.StringMessageUtils;
import org.mule.util.SystemUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractTransformer</code> is a base class for all transformers.
 * Transformations transform one object into another.
 */

public abstract class AbstractTransformer extends AbstractAnnotatedObject implements Transformer
{
    public static final DataType<MuleMessage> MULE_MESSAGE_DATA_TYPE = new SimpleDataType<MuleMessage>(MuleMessage.class);

    protected MuleContext muleContext;

    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * The return type that will be returned by the {@link #transform} method is
     * called
     */
    protected DataType<?> returnType = new SimpleDataType<Object>(Object.class);

    /**
     * The name that identifies this transformer. If none is set the class name of
     * the transformer is used
     */
    protected String name = null;

    /**
     * The endpoint that this transformer instance is configured on
     */
    protected ImmutableEndpoint endpoint = null;

    /**
     * A list of supported Class types that the source payload passed into this
     * transformer
     */
    protected final List<DataType<?>> sourceTypes = new CopyOnWriteArrayList<DataType<?>>();

    /**
     * Determines whether the transformer will throw an exception if the message
     * passed is is not supported
     */
    private boolean ignoreBadInput = false;

    /**
     * Allows a transformer to return a null result
     */
    private boolean allowNullReturn = false;

    /*
     *  Mime type and encoding for transformer output
     */
    protected String mimeType;
    protected String encoding;

    /**
     * default constructor required for discovery
     */
    public AbstractTransformer()
    {
        super();
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (event != null && event.getMessage() != null)
        {
            try
            {
                MuleMessage message = event.getMessage();
                message.applyTransformers(event, this);
                if (message instanceof DefaultMessageCollection)
                {
                    if (((DefaultMessageCollection) message).isInvalidatedPayload())
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Transformed message is an invalidated message collection. Creating new message with payload: " + event.getMessage().getPayload());
                        }
                        MuleMessage newMessage = new DefaultMuleMessage(message.getPayload(), message, message.getMuleContext());
                        event = new DefaultMuleEvent(newMessage, event);
                    }
                }
            }
            catch (Exception e)
            {
                throw new TransformerMessagingException(event, this, e);
            }
        }
        return event;
    }

    /**
     * Register a supported data type with this transformer.  The will allow objects that match this data type to be
     * transformed by this transformer.
     *
     * @param aClass the source type to allow
     * @deprecated use registerSourceType(DataType)
     */
    @Deprecated
    protected void registerSourceType(Class<?> aClass)
    {
        registerSourceType(new SimpleDataType<Object>(aClass));
    }

    /**
     * Unregister a supported source type from this transformer
     *
     * @param aClass the type to remove
     * @deprecated use unregisterSourceType(DataType)
     */
    @Deprecated
    protected void unregisterSourceType(Class<?> aClass)
    {
        unregisterSourceType(new SimpleDataType<Object>(aClass));
    }

    /**
     * Register a supported data type with this transformer.  The will allow objects that match this data type to be
     * transformed by this transformer.
     *
     * @param dataType the source type to allow
     */
    protected void registerSourceType(DataType<?> dataType)
    {
        if (!sourceTypes.contains(dataType))
        {
            sourceTypes.add(dataType);

            if (dataType.getType().equals(Object.class))
            {
                logger.debug("java.lang.Object has been added as source type for this transformer, there will be no source type checking performed");
            }
        }
    }

    /**
     * Unregister a supported source type from this transformer
     *
     * @param dataType the type to remove
     */
    protected void unregisterSourceType(DataType<?> dataType)
    {
        sourceTypes.remove(dataType);
    }

    /**
     * @return transformer name
     */
    @Override
    public String getName()
    {
        if (name == null)
        {
            name = this.generateTransformerName();
        }
        return name;
    }

    /**
     * @param string
     */
    @Override
    public void setName(String string)
    {
        if (string == null)
        {
            string = ClassUtils.getSimpleName(this.getClass());
        }

        logger.debug("Setting transformer name to: " + string);
        name = string;
    }

    @Deprecated
    @Override
    public Class<?> getReturnClass()
    {
        return returnType.getType();
    }

    @Override
    public void setReturnDataType(DataType<?> type)
    {
        this.returnType = type.cloneDataType();
        this.encoding = type.getEncoding();
        this.mimeType = type.getMimeType();
    }

    @Override
    public DataType<?> getReturnDataType()
    {
        return returnType;
    }

    @Override
    @Deprecated
    public void setReturnClass(Class<?> newClass)
    {
        DataType<?> tempReturnType = new SimpleDataType<Object>(newClass);
        tempReturnType.setMimeType(mimeType);
        tempReturnType.setEncoding(encoding);
        setReturnDataType(tempReturnType);
    }

    public void setMimeType(String mimeType) throws MimeTypeParseException
    {
        if (mimeType == null)
        {
            this.mimeType = null;
        }
        else
        {
            MimeType mt = new MimeType(mimeType);
            this.mimeType = mt.getPrimaryType() + "/" + mt.getSubType();
        }
        if (returnType != null)
        {
            returnType.setMimeType(mimeType);
        }
    }

    @Override
    public String getMimeType()
    {
        return mimeType;
    }

    @Override
    public String getEncoding()
    {
        return encoding;
    }

    public void setEncoding(String encoding)
    {
        this.encoding = encoding;
        if (returnType != null)
        {
            returnType.setEncoding(encoding);
        }
    }

    public boolean isAllowNullReturn()
    {
        return allowNullReturn;
    }

    public void setAllowNullReturn(boolean allowNullReturn)
    {
        this.allowNullReturn = allowNullReturn;
    }

    @Deprecated
    @Override
    public boolean isSourceTypeSupported(Class<?> aClass)
    {
        return isSourceDataTypeSupported(DataTypeFactory.create(aClass), false);
    }

    @Override
    public boolean isSourceDataTypeSupported(DataType<?> dataType)
    {
        return isSourceDataTypeSupported(dataType, false);
    }

    /**
     * Determines whether that data type passed in is supported by this transformer
     *
     * @param aClass     the type to check against
     * @param exactMatch if the source type on this transformer is open (can be anything) it will return true unless an
     *                   exact match is requested using this flag
     * @return true if the source type is supported by this transformer, false otherwise
     * @deprecated use {@link #isSourceDataTypeSupported(org.mule.api.transformer.DataType, boolean)}
     */
    @Deprecated
    public boolean isSourceTypeSupported(Class<MuleMessage> aClass, boolean exactMatch)
    {
        return isSourceDataTypeSupported(new SimpleDataType<MuleMessage>(aClass), exactMatch);
    }

    /**
     * Determines whether that data type passed in is supported by this transformer
     *
     * @param dataType   the type to check against
     * @param exactMatch if set to true, this method will look for an exact match to the data type, if false it will look
     *                   for a compatible data type.
     * @return true if the source type is supported by this transformer, false otherwise
     */
    public boolean isSourceDataTypeSupported(DataType<?> dataType, boolean exactMatch)
    {
        int numTypes = sourceTypes.size();

        if (numTypes == 0)
        {
            return !exactMatch;
        }

        for (DataType<?> sourceType : sourceTypes)
        {
            if (exactMatch)
            {
                if (sourceType.equals(dataType))
                {
                    return true;
                }
            }
            else
            {
                if (sourceType.isCompatibleWith(dataType))
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public final Object transform(Object src) throws TransformerException
    {
        String enc = encoding != null ? encoding : getEncoding(src);
        return transform(src, enc);
    }

    @Override
    public Object transform(Object src, String enc) throws TransformerException
    {
        Object payload = src;
        if (src instanceof MuleMessage)
        {
            MuleMessage message = (MuleMessage) src;
            if ((!isSourceDataTypeSupported(MULE_MESSAGE_DATA_TYPE, true) &&
                 !(this instanceof AbstractMessageTransformer)))
            {
                src = ((MuleMessage) src).getPayload();
                payload = message.getPayload();
            }
        }

        DataType<?> sourceType = DataTypeFactory.create(payload.getClass());
        //Once we support mime types, it should be possible to do this since we'll be able to discern the difference
        //between objects with the same type
//        if(getReturnDataType().isCompatibleWith(sourceType))
//        {
//            logger.debug("Object is already of type: " + getReturnDataType() + " No transform to perform");
//            return payload;
//        }

        if (!isSourceDataTypeSupported(sourceType))
        {
            if (ignoreBadInput && !useExtendedTransformations())
            {
                logger.debug("Source type is incompatible with this transformer and property 'ignoreBadInput' is set to true, so the transformer chain will continue.");
                return payload;
            }
            else
            {
                Message msg = CoreMessages.transformOnObjectUnsupportedTypeOfEndpoint(getName(),
                    payload.getClass(), endpoint);
                /// FIXME
                throw new TransformerException(msg, this);
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("Applying transformer %s (%s)", getName(), getClass().getName()));
            logger.debug(String.format("Object before transform: %s", StringMessageUtils.toString(payload)));
        }

        Object result = doTransform(payload, enc);

        if (result == null)
        {
            result = NullPayload.getInstance();
        }

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("Object after transform: %s", StringMessageUtils.toString(result)));
        }

        TransformerUtils.checkTransformerReturnClass(this, result);

        return result;
    }

    private boolean useExtendedTransformations()
    {
        boolean result = true;
        if (muleContext != null && muleContext.getConfiguration() != null)
        {
            result = muleContext.getConfiguration().useExtendedTransformations();
        }

        return result;
    }


    protected String getEncoding(Object src)
    {
        String enc = null;
        if (src instanceof MuleMessage)
        {
            enc = ((MuleMessage) src).getEncoding();
        }

        if (enc == null && endpoint != null)
        {
            enc = endpoint.getEncoding();
        }
        else if (enc == null)
        {
            enc = SystemUtils.getDefaultEncoding(muleContext);
        }
        return enc;
    }

    protected boolean isConsumed(Class<?> srcCls)
    {
        return InputStream.class.isAssignableFrom(srcCls) || StreamSource.class.isAssignableFrom(srcCls);
    }

    @Override
    public ImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }

    @Override
    public void setEndpoint(ImmutableEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    protected abstract Object doTransform(Object src, String enc) throws TransformerException;

    /**
     * Template method where deriving classes can do any initialisation after the
     * properties have been set on this transformer
     *
     * @throws InitialisationException
     */
    @Override
    public void initialise() throws InitialisationException
    {
        // do nothing, subclasses may override
    }

    /**
     * Template method where deriving classes can do any clean up any resources or state
     * before the object is disposed.
     */
    @Override
    public void dispose()
    {
        // do nothing, subclasses may override
    }

    protected String generateTransformerName()
    {
        return TransformerUtils.generateTransformerName(getClass(), returnType);
    }

    @Deprecated
    @Override
    public List<Class<?>> getSourceTypes()
    {
        //A work around to support the legacy API
        List<Class<?>> sourceClasses = new ArrayList<Class<?>>();
        for (DataType<?> sourceType : sourceTypes)
        {
            sourceClasses.add(sourceType.getType());
        }
        return Collections.unmodifiableList(sourceClasses);
    }

    @Override
    public List<DataType<?>> getSourceDataTypes()
    {
        return Collections.unmodifiableList(sourceTypes);
    }

    @Override
    public boolean isIgnoreBadInput()
    {
        return ignoreBadInput;
    }

    public void setIgnoreBadInput(boolean ignoreBadInput)
    {
        this.ignoreBadInput = ignoreBadInput;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(80);
        sb.append(ClassUtils.getSimpleName(this.getClass()));
        sb.append("{this=").append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(", name='").append(name).append('\'');
        sb.append(", ignoreBadInput=").append(ignoreBadInput);
        sb.append(", returnClass=").append(returnType);
        sb.append(", sourceTypes=").append(sourceTypes);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean isAcceptNull()
    {
        return false;
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}
