/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers;

import org.mule.impl.VoidTransformer;
import org.mule.providers.service.TransportFactoryException;
import org.mule.providers.service.TransportServiceDescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.CollectionUtils;

import edu.emory.mathcs.backport.java.util.Collections;

import java.util.Iterator;
import java.util.List;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TransformerUtils
{

    // Undefined transformers are "special" - they seem to indicate that either the endpoint is under
    // construction, or that a value will be supplied later.  They are not the same as an empty list,
    // which means "no transformer to be used" rather than "some special state".

    // In the future we could test for an warn on UNDEFINED being passed to the "apply" methods
    // below.  This is either an error or, in some cases, seems to be related to very late binding
    // of transformers (see code in MuleClient, for example

    // Null values for the transformer list are avoided completely in an attempt to make the code
    // clearer.

    public static final List UNDEFINED =
            Collections.unmodifiableList(
                    CollectionUtils.singletonList(VoidTransformer.getInstance()));
    
    private static Log logger = LogFactory.getLog(AbstractTransformer.class);

    public static void initialiseAllTransformers(List transformers) throws InitialisationException
    {
        if (isDefined(transformers))
        {
            Iterator transformer = transformers.iterator();
            while (transformer.hasNext())
            {
                ((UMOTransformer) transformer.next()).initialise();
            }
        }
    }

    public static String toString(List transformers)
    {
        StringBuffer buffer = new StringBuffer();
        if (isDefined(transformers))
        {
            Iterator transformer = transformers.iterator();
            while (transformer.hasNext())
            {
                buffer.append(transformer.next().toString());
                if (transformer.hasNext())
                {
                    buffer.append(" -> ");
                }
            }
        }
        return buffer.toString();
    }

    public static UMOTransformer firstOrNull(List transformers)
    {
        if (isDefined(transformers) && 0 != transformers.size())
        {
            return (UMOTransformer) transformers.get(0);
        }
        else
        {
            return null;
        }
    }

    public static boolean isSourceTypeSupportedByFirst(List transformers, Class clazz)
    {
        UMOTransformer transformer = firstOrNull(transformers);
        return null != transformer && transformer.isSourceTypeSupported(clazz);
    }

    public static boolean isUndefined(List transformers)
    {
        discourageNullTransformers(transformers);
        // pointer equality
        return null == transformers || UNDEFINED == transformers;
    }

    public static void discourageNullTransformers(List transformers)
    {
        // please don't use null for undefined transformers
        if (null == transformers)
        {
            if (logger.isWarnEnabled())
            {
                logger.warn("Null transformer detected - please use UNDEFINED or empty list, as appropriate");
            }
//            throw new NullPointerException("Null transformer detected - please use UNDEFINED or empty list, as appropriate");
        }
    }

    public static boolean isDefined(List transformers)
    {
        return !isUndefined(transformers);
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
        catch (UMOException e)
        {
            logger.debug(e.getMessage(), e);
            return TransformerUtils.UNDEFINED;
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

}
