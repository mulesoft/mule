/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transformer;

import org.mule.api.DefaultMuleException;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;
import org.mule.transport.service.TransportFactoryException;
import org.mule.transport.service.TransportServiceDescriptor;
import org.mule.util.ClassUtils;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TransformerUtils
{

    public static final String COMMA = ",";

    private static Log logger = LogFactory.getLog(AbstractTransformer.class);

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
        StringBuffer buffer = new StringBuffer();
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
            logger.debug(e.getMessage(), e);
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

        if (logger.isDebugEnabled())
        {
            logger.debug("The transformed value is of expected type. Type is: " + ClassUtils.getSimpleName(value.getClass()));
        }
    }
}
