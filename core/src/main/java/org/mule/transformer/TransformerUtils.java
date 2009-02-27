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

import org.mule.RegistryContext;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.service.TransportFactoryException;
import org.mule.transport.service.TransportServiceDescriptor;

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

    public static void initialiseAllTransformers(List transformers) throws InitialisationException
    {
        if (transformers != null)
        {
            Iterator transformer = transformers.iterator();
            while (transformer.hasNext())
            {
                ((Transformer) transformer.next()).initialise();
            }
        }
    }

    public static String toString(List transformers)
    {
        StringBuffer buffer = new StringBuffer();
            Iterator transformer = transformers.iterator();
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

    public static Transformer firstOrNull(List transformers)
    {
        if (transformers != null && 0 != transformers.size())
        {
            return (Transformer) transformers.get(0);
        }
        else
        {
            return null;
        }
    }

    public static boolean isSourceTypeSupportedByFirst(List transformers, Class clazz)
    {
        Transformer transformer = firstOrNull(transformers);
        return null != transformer && transformer.isSourceTypeSupported(clazz);
    }

    protected static interface TransformerSource
    {
        public List getTransformers() throws TransportFactoryException;
    }

    protected static List getTransformersFromSource(TransformerSource source)
    {
        try
        {
            List transformers = source.getTransformers();
            TransformerUtils.initialiseAllTransformers(transformers);
            return transformers;
        }
        catch (MuleException e)
        {
            logger.debug(e.getMessage(), e);
            return null;
        }
    }

    public static List getDefaultInboundTransformers(final TransportServiceDescriptor serviceDescriptor)
    {
        return getTransformersFromSource(new TransformerSource()
        {
            public List getTransformers() throws TransportFactoryException
            {
                return serviceDescriptor.createInboundTransformers();
            }
        });
    }

    public static List getDefaultResponseTransformers(final TransportServiceDescriptor serviceDescriptor)
    {
        return getTransformersFromSource(new TransformerSource()
        {
            public List getTransformers() throws TransportFactoryException
            {
                return serviceDescriptor.createResponseTransformers();
            }
        });
    }

    public static List getDefaultOutboundTransformers(final TransportServiceDescriptor serviceDescriptor)
    {
        return getTransformersFromSource(new TransformerSource()
        {
            public List getTransformers() throws TransportFactoryException
            {
                return serviceDescriptor.createOutboundTransformers();
            }
        });
    }

    /**
     * Builds a list of Transformers.
     *
     * @param names - a list of transformers separated by commans
     * @return a list (possibly empty) of transformers or
     * @throws org.mule.api.DefaultMuleException
     */
    public static List getTransformers(String names) throws DefaultMuleException
    {
        if (null != names)
        {
            List transformers = new LinkedList();
            StringTokenizer st = new StringTokenizer(names, COMMA);
            while (st.hasMoreTokens())
            {
                String key = st.nextToken().trim();
                Transformer transformer = RegistryContext.getRegistry().lookupTransformer(key);

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

}
