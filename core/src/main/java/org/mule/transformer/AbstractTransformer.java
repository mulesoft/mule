/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.MessageAdapter;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.types.CollectionDataType;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.SimpleDataType;
import org.mule.transport.NullPayload;
import org.mule.util.ClassUtils;
import org.mule.util.StringMessageUtils;
import org.mule.util.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.transform.stream.StreamSource;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>AbstractTransformer</code> is a base class for all transformers.
 * Transformations transform one object into another.
 */

public abstract class AbstractTransformer implements Transformer
{
    public static final DataType MULE_MESSAGE_DATA_TYPE = new SimpleDataType(MuleMessage.class);
    public static final DataType MULE_MESSAGE_ADAPTER_DATA_TYPE = new SimpleDataType(MessageAdapter.class);

    protected static final int DEFAULT_TRUNCATE_LENGTH = 200;

    protected MuleContext muleContext;

    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * The return type that will be returned by the {@link #transform} method is
     * called
     */
    protected DataType returnType = new SimpleDataType(Object.class);

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
    @SuppressWarnings("unchecked")
    protected final List<DataType<?>> sourceTypes = new CopyOnWriteArrayList/*<DataType>*/();

    /**
     * Determines whether the transformer will throw an exception if the message
     * passed is is not supported or the return tye is incorrect
     */
    private boolean ignoreBadInput = false;

    /**
     * default constructor required for discovery
     */
    public AbstractTransformer()
    {
        super();
    }

    protected Object checkReturnClass(Object object) throws TransformerException
    {
        if (returnType != null)
        {
            DataType dt;
            if (object instanceof Collection)
            {
                dt = new CollectionDataType((Class<? extends Collection>) object.getClass(), DataType.ANY_MIME_TYPE);
            }
            else
            {
                dt = new SimpleDataType(object.getClass(), DataType.ANY_MIME_TYPE);
            }
            if (!returnType.isCompatibleWith(dt))
            {
                throw new TransformerException(
                        CoreMessages.transformUnexpectedType(dt, returnType),
                        this);
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("The transformed object is of expected type. Type is: " +
                    ClassUtils.getSimpleName(object.getClass()));
        }

        return object;
    }

    /**
     * Register a supported data type with this transformer.  The will allow objects that match this data type to be
     * transformed by this transformer.
     *
     * @param aClass the source type to allow
     */
    protected void registerSourceType(Class<?> aClass)
    {
        registerSourceType(new SimpleDataType(aClass));
    }

    /**
     * Unregister a supported source type from this transformer
     *
     * @param aClass the type to remove
     */
    protected void unregisterSourceType(Class<?> aClass)
    {
        unregisterSourceType(new SimpleDataType(aClass));
    }

    /**
     * Register a supported data type with this transformer.  The will allow objects that match this data type to be
     * transformed by this transformer.
     *
     * @param dataType the source type to allow
     */
    protected void registerSourceType(DataType dataType)
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
    protected void unregisterSourceType(DataType dataType)
    {
        sourceTypes.remove(dataType);
    }

    /**
     * @return transformer name
     */
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
    public void setName(String string)
    {
        if (string == null)
        {
            string = ClassUtils.getSimpleName(this.getClass());
        }

        logger.debug("Setting transformer name to: " + string);
        name = string;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.transformer.Transformer#getReturnClass()
     */
    public Class getReturnClass()
    {
        return returnType.getType();
    }

    public void setReturnDataType(DataType type)
    {
        this.returnType = type;
    }

    public DataType getReturnDataType()
    {
        return returnType;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.transformer.Transformer#setReturnClass(java.lang.String)
     */
    public void setReturnClass(Class newClass)
    {
        returnType = new SimpleDataType(newClass);
    }

    public boolean isSourceTypeSupported(Class aClass)
    {
        return isSourceDataTypeSupported(new SimpleDataType(aClass), false);
    }

    public boolean isSourceDataTypeSupported(DataType dataType)
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
    public boolean isSourceTypeSupported(Class aClass, boolean exactMatch)
    {
        return isSourceDataTypeSupported(new SimpleDataType(aClass), exactMatch);
    }

    /**
     * Determines whether that data type passed in is supported by this transformer
     *
     * @param dataType   the type to check against
     * @param exactMatch if set to true, this method will look for an exact match to the data type, if false it will look
     *                   for a compatible data type.
     * @return true if the source type is supported by this transformer, false otherwise
     */
    public boolean isSourceDataTypeSupported(DataType dataType, boolean exactMatch)
    {
        int numTypes = sourceTypes.size();

        if (numTypes == 0)
        {
            return !exactMatch;
        }

        for (DataType sourceType : sourceTypes)
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

    public final Object transform(Object src) throws TransformerException
    {
        return transform(src, getEncoding(src));
    }

    public Object transform(Object src, String encoding) throws TransformerException
    {
        Object payload = src;
        MessageAdapter adapter;
        if (src instanceof MessageAdapter)
        {

            adapter = (MessageAdapter) src;
            if ((!isSourceDataTypeSupported(MULE_MESSAGE_ADAPTER_DATA_TYPE, true)
                    && !isSourceDataTypeSupported(MULE_MESSAGE_DATA_TYPE, true)
                    && !(this instanceof AbstractMessageAwareTransformer))
                    )
            {
                src = ((MessageAdapter) src).getPayload();
                payload = adapter.getPayload();
            }
        }

        DataType sourceType = new DataTypeFactory().create(payload.getClass());
        //Once we support mime types, it should be possible to do this since we'll be able to discern the difference
        //between objects with the same type
//        if(getReturnDataType().isCompatibleWith(sourceType))
//        {
//            logger.debug("Object is already of type: " + getReturnDataType() + " No transform to perform");
//            return payload;
//        }

        if (!isSourceDataTypeSupported(sourceType))
        {
            if (ignoreBadInput)
            {
                logger.debug("Source type is incompatible with this transformer and property 'ignoreBadInput' is set to true, so the transformer chain will continue.");
                return payload;
            }
            else
            {
                throw new TransformerException(
                        CoreMessages.transformOnObjectUnsupportedTypeOfEndpoint(this.getName(),
                                payload.getClass(), endpoint), this);
            }
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Applying transformer " + getName() + " (" + getClass().getName() + ")");
            logger.debug("Object before transform: "
                    + StringMessageUtils.truncate(StringMessageUtils.toString(payload), DEFAULT_TRUNCATE_LENGTH, false));
        }

        Object result;
        result = doTransform(payload, encoding);
        // }
        if (result == null)
        {
            result = NullPayload.getInstance();
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Object after transform: "
                    + StringMessageUtils.truncate(StringMessageUtils.toString(result), DEFAULT_TRUNCATE_LENGTH, false));
        }

        result = checkReturnClass(result);
        return result;
    }

    protected String getEncoding(Object src)
    {
        String encoding = null;
        if (src instanceof MessageAdapter)
        {
            encoding = ((MessageAdapter) src).getEncoding();
        }

        if (encoding == null && endpoint != null)
        {
            encoding = endpoint.getEncoding();
        }
        else if (encoding == null)
        {
            encoding = System.getProperty(MuleProperties.MULE_ENCODING_SYSTEM_PROPERTY);
        }
        return encoding;
    }

    protected boolean isConsumed(Class srcCls)
    {
        return InputStream.class.isAssignableFrom(srcCls) || StreamSource.class.isAssignableFrom(srcCls);
    }

    public ImmutableEndpoint getEndpoint()
    {
        return endpoint;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.mule.api.transformer.Transformer#setConnector(org.mule.api.transport.Connector)
     */
    public void setEndpoint(ImmutableEndpoint endpoint)
    {
        this.endpoint = endpoint;
    }

    protected abstract Object doTransform(Object src, String encoding) throws TransformerException;

    /**
     * Template method where deriving classes can do any initialisation after the
     * properties have been set on this transformer
     *
     * @throws InitialisationException
     */
    public void initialise() throws InitialisationException
    {
        // do nothing, subclasses may override
    }

    protected String generateTransformerName()
    {
        String name = ClassUtils.getSimpleName(this.getClass());
        int i = name.indexOf("To");
        if (i > 0 && returnType != null)
        {
            String target = ClassUtils.getSimpleName(returnType.getType());
            if (target.equals("byte[]"))
            {
                target = "byteArray";
            }
            name = name.substring(0, i + 2) + StringUtils.capitalize(target);
        }
        return name;
    }

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

    public List<DataType<?>> getSourceDataTypes()
    {
        return Collections.unmodifiableList(sourceTypes);
    }

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
        StringBuffer sb = new StringBuffer(80);
        sb.append(ClassUtils.getSimpleName(this.getClass()));
        sb.append("{this=").append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(", name='").append(name).append('\'');
        sb.append(", ignoreBadInput=").append(ignoreBadInput);
        sb.append(", returnClass=").append(returnType);
        sb.append(", sourceTypes=").append(sourceTypes);
        sb.append('}');
        return sb.toString();
    }

    public boolean isAcceptNull()
    {
        return false;
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }
}
