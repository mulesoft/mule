/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transformer;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.Converter;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;
import org.mule.transport.service.TransportFactoryException;
import org.mule.transport.service.TransportServiceDescriptor;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

import static org.mule.transformer.TransformerUtils.generateTransformerName;
import static org.mule.transformer.TransformerUtils.getTransformationLength;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransformerUtils
{

    private static final String LENGTH_STRING = "_length_";
    private static Logger LOGGER = LoggerFactory.getLogger(AbstractTransformer.class);
    public static final String COMMA = ",";

    public static void initialiseAllTransformers(List<Transformer> transformers) throws InitialisationException
    {
        if (transformers != null)
        {
            Iterator<Transformer> transformer = transformers.iterator();
            while (transformer.hasNext())
            {
                (transformer.next()).initialise();
            }
        }
    }

    public static String toString(List<Transformer> transformers)
    {
        StringBuilder buffer = new StringBuilder();
        Iterator<Transformer> transformer = transformers.iterator();
        while (transformer.hasNext())
        {
            buffer.append(transformer.next().toString());
            if (transformer.hasNext())
            {
                buffer.append(" -> ");
            }
        }
        return buffer.toString();
    }

    public static Transformer firstOrNull(List<Transformer> transformers)
    {
        if (transformers != null && 0 != transformers.size())
        {
            return transformers.get(0);
        }
        else
        {
            return null;
        }
    }

    public static boolean isSourceTypeSupportedByFirst(List<Transformer> transformers, Class clazz)
    {
        Transformer transformer = firstOrNull(transformers);
        return null != transformer && transformer.isSourceDataTypeSupported(new DataTypeFactory().create(clazz));
    }

    protected static interface TransformerSource
    {
        public List<Transformer> getTransformers() throws TransportFactoryException;
    }

    protected static List<Transformer> getTransformersFromSource(TransformerSource source)
    {
        try
        {
            List<Transformer> transformers = source.getTransformers();
            TransformerUtils.initialiseAllTransformers(transformers);
            return transformers;
        }
        catch (MuleException e)
        {
            LOGGER.debug(e.getMessage(), e);
            return null;
        }
    }

    public static List<Transformer> getDefaultInboundTransformers(final TransportServiceDescriptor serviceDescriptor, final ImmutableEndpoint endpoint)
    {
        return getTransformersFromSource(new TransformerSource()
        {
            public List<Transformer> getTransformers() throws TransportFactoryException
            {
                return serviceDescriptor.createInboundTransformers(endpoint);
            }
        });
    }

    public static List<Transformer> getDefaultResponseTransformers(final TransportServiceDescriptor serviceDescriptor, final ImmutableEndpoint endpoint)
    {
        return getTransformersFromSource(new TransformerSource()
        {
            public List<Transformer> getTransformers() throws TransportFactoryException
            {
                return serviceDescriptor.createResponseTransformers(endpoint);
            }
        });
    }

    public static List<Transformer> getDefaultOutboundTransformers(final TransportServiceDescriptor serviceDescriptor, final ImmutableEndpoint endpoint)
    {
        return getTransformersFromSource(new TransformerSource()
        {
            public List<Transformer> getTransformers() throws TransportFactoryException
            {
                return serviceDescriptor.createOutboundTransformers(endpoint);
            }
        });
    }

    /**
     * Builds a list of Transformers.
     *
     * @param names - a list of transformers separated by commands
     * @param muleContext the current muleContext. This is used to look up transformers in the registry
     * @return a list (possibly empty) of transformers or
     * @throws org.mule.api.DefaultMuleException if any of the transformers cannot be found
     */
    public static List<Transformer> getTransformers(String names, MuleContext muleContext) throws DefaultMuleException
    {
        if (null != names)
        {
            List<Transformer> transformers = new LinkedList<Transformer>();
            StringTokenizer st = new StringTokenizer(names, COMMA);
            while (st.hasMoreTokens())
            {
                String key = st.nextToken().trim();
                Transformer transformer = muleContext.getRegistry().lookupTransformer(key);

                if (transformer == null)
                {
                    throw new DefaultMuleException(CoreMessages.objectNotRegistered("Transformer", key));
                }
                transformers.add(transformer);
            }
            return transformers;
        }
        else
        {
            return null;
        }
    }

    /**
     * Checks whether a given value is a valid output for a transformer.
     *
     * @param transformer the transformer used to validate
     * @param value the output value
     * @throws TransformerException if the out[ut value is of a unexpected type.
     */
    public static void checkTransformerReturnClass(Transformer transformer, Object value) throws TransformerException
    {
        if (value == null || value instanceof NullPayload && (transformer instanceof AbstractTransformer &&((AbstractTransformer) transformer).isAllowNullReturn()))
        {
            return;
        }

        if (transformer.getReturnDataType() != null)
        {
            DataType<?> dt = DataTypeFactory.create(value.getClass());
            if (!transformer.getReturnDataType().isCompatibleWith(dt))
            {
                throw new TransformerException(
                        CoreMessages.transformUnexpectedType(dt, transformer.getReturnDataType()),
                        transformer);
            }
        }

        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("The transformed value is of expected type. Type is: " + ClassUtils.getSimpleName(value.getClass()));
        }
    }

    public static <T> Object transformToAny(T input, MuleContext muleContext, DataType<?>... supportedTypes)
    {
        final DataType sourceType = DataTypeFactory.create(input.getClass());
        Object transformedData = null;

        for (DataType<?> supportedType : supportedTypes)
        {
            transformedData = attemptTransformation(sourceType, input, supportedType, muleContext);
            if (transformedData != null)
            {
                break;
            }
        }

        return transformedData;
    }

    private static <S, R> R attemptTransformation(DataType<S> sourceDataType, S source, DataType<R> resultDataType, MuleContext muleContext)
    {
        Transformer transformer;
        try
        {
            transformer = muleContext.getRegistry().lookupTransformer(sourceDataType, resultDataType);
        }
        catch (TransformerException e)
        {
            LOGGER.debug("Could not find a transformer from type {} to {}", sourceDataType.getType().getName(), resultDataType.getType().getName());
            return null;
        }

        LOGGER.debug("Located transformer {} from type {} to type {}. Attempting transformation...", transformer.getName(), sourceDataType.getType().getName(), resultDataType.getType().getName());

        try
        {
            return (R) transformer.transform(source);
        }
        catch (TransformerException e)
        {
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug(
                        String.format("Transformer %s threw exception while trying to transform an object of type %s into a %s",
                                      transformer.getName(), sourceDataType.getType().getName(), resultDataType.getType().getName())
                        , e);
            }

            return null;
        }
    }

    public static String generateTransformerName(Class<? extends Transformer> transformerClass, DataType returnType)
    {
        String transformerName = ClassUtils.getSimpleName(transformerClass);
        int i = transformerName.indexOf("To");
        if (i > 0 && returnType != null)
        {
            String target = ClassUtils.getSimpleName(returnType.getType());
            if (target.equals("byte[]"))
            {
                target = "byteArray";
            }
            transformerName = transformerName.substring(0, i + 2) + StringUtils.capitalize(target);
        }
        return transformerName;
    }
    
    public static int getTransformationLength(Transformer transformer)
    {
        if (transformer instanceof CompositeConverter)
        {
            return ((CompositeConverter) transformer).getConverters().size();
        }
        else
        {
            return 1;
        }
    }

    public static String getConverterKey(Transformer t)
    {
        return t.getName() != null ? t.getName() + LENGTH_STRING + getTransformationLength(t) : generateTransformerName(t.getClass(), t.getReturnDataType()) + LENGTH_STRING + getTransformationLength(t);
    }
}
