/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.transformer.jaxb;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.registry.RegistrationException;
import org.mule.api.registry.ResolverException;
import org.mule.api.registry.TransformerResolver;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.simple.ObjectToString;
import org.mule.util.annotation.AnnotationUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO
 */
public class JAXBTransformerResolver implements TransformerResolver, MuleContextAware, Disposable
{
    public static final String[] ignorredPackages = {"java.,javax.,org.w3c.,org.mule.transport."};

    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(JAXBTransformerResolver.class);

    private MuleContext muleContext;

    //We cache the the transformers, this will get cleared when the server shuts down
    private Map<String, Transformer> transformerCache = new ConcurrentHashMap<String, Transformer>();

    private JAXBContextResolver resolver;

    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    public Transformer resolve(DataType source, DataType result) throws ResolverException
    {
        Transformer t = transformerCache.get(source.toString() + result.toString());
        if (t != null)
        {
            return t;
        }

        try
        {
            JAXBContext jaxb = getContextResolver().resolve(JAXBContext.class, source, result, muleContext);

            if(jaxb==null)
            {
                return null;
            }
            boolean marshal = false;
            Class annotatedType = null;


            if (getContextResolver().getMatchingClasses().contains(result.getType()))
            {
                annotatedType = result.getType();
                marshal = false;
            }
            else
            {
                if (getContextResolver().getMatchingClasses().contains(source.getType()))
                {
                    annotatedType = source.getType();
                    marshal = true;
                }
            }

            if (annotatedType == null)
            {
                annotatedType = result.getType();
                boolean isJAXB = hasJaxbAnnotations(annotatedType);
                if (!isJAXB)
                {
                    marshal = true;
                    annotatedType = source.getType();
                    isJAXB = hasJaxbAnnotations(annotatedType);
                }

                if (!isJAXB)
                {
                    return null;
                }
            }
            //At this point we know we are dealing with JAXB, now lets check the registry to see if there is an exact
            //transformer that matches our criteria
            List<Transformer> ts = muleContext.getRegistry().lookupTransformers(source, result);
            if (ts.size() == 1 && !(ts.get(0) instanceof ObjectToString))
            {
                t = ts.get(0);
            }
            else
            {
                if (marshal)
                {
                    t = new JAXBMarshallerTransformer(jaxb, result);
                }
                else
                {
                    t = new JAXBUnmarshallerTransformer(jaxb, result);
                }
            }

            transformerCache.put(source.toString() + result.toString(), t);
            return t;

        }
        catch (Exception e)
        {
            throw new ResolverException(CoreMessages.createStaticMessage("Failed to unmarshal"), e);
        }
    }

    public void transformerChange(Transformer transformer, RegistryAction registryAction)
    {
        //nothing to do
    }

    public void dispose()
    {
        transformerCache.clear();
    }

    protected JAXBContextResolver getContextResolver() throws RegistrationException
    {
        if(resolver==null)
        {
            resolver = muleContext.getRegistry().lookupObject(JAXBContextResolver.class);
        }
        return resolver;
    }

    protected boolean hasJaxbAnnotations(Class annotatedType)
    {
        String p = annotatedType.getPackage().getName();
        for (int i = 0; i < ignorredPackages.length; i++)
        {
            if(p.startsWith(ignorredPackages[i])) return false;
        }

        try
        {
             return AnnotationUtils.hasAnnotationWithPackage("javax.xml.bind.annotation", annotatedType);
        }
        catch (IOException e)
        {
            logger.warn("Failed to scan class for Jaxb annotations: " + annotatedType, e);
            return false;
        }
    }
}
