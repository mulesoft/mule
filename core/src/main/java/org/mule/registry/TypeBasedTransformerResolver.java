/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.ResolverException;
import org.mule.api.registry.TransformCriteria;
import org.mule.api.registry.TransformerResolver;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.CoreMessages;
import org.mule.transformer.TransformerChain;
import org.mule.transformer.TransformerWeighting;
import org.mule.transformer.simple.ObjectToByteArray;
import org.mule.transformer.simple.ObjectToString;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Will discover transformers based on type information only. IT looks for transformers that support the source and result
 * types passed into the method.  This resolver only resolves on the first source type ,which is the way transformer
 * resolution working in Mule 2.x.
 */
public class TypeBasedTransformerResolver implements TransformerResolver, MuleContextAware, Disposable, Initialisable
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(TypeBasedTransformerResolver.class);

    private ObjectToString objectToString;
    private ObjectToByteArray objectToByteArray;

    private MuleContext muleContext;

    protected Map<String, Transformer> exactTransformerCache = new ConcurrentHashMap/*<String, Transformer>*/(8);


    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public void initialise() throws InitialisationException
    {
        objectToString = new ObjectToString();
        objectToByteArray = new ObjectToByteArray();
    }

    public Transformer resolve(TransformCriteria criteria) throws ResolverException
    {
        Transformer result = exactTransformerCache.get(criteria.getInputTypes()[0].getName() + criteria.getOutputType().getName());
        if (result != null)
        {
            return result;
        }

        List trans = muleContext.getRegistry().lookupTransformers(criteria.getInputTypes()[0], criteria.getOutputType());

        result = getNearestTransformerMatch(trans, criteria.getInputTypes()[0], criteria.getOutputType());
        //If an exact mach is not found, we have a 'second pass' transformer that can be used to converting to String or
        //byte[]
        Transformer secondPass = null;

        if (result == null)
        {
            //If no transformers were found but the outputType type is String or byte[] we can perform a more general search
            // using Object.class and then convert to String or byte[] using the second pass transformer
            if (criteria.getOutputType().equals(String.class))
            {
                secondPass = objectToString;
            }
            else if (criteria.getOutputType().equals(byte[].class))
            {
                secondPass = objectToByteArray;
            }
            else
            {
                return null;
            }
            //Perform a more general search
            trans = muleContext.getRegistry().lookupTransformers(criteria.getInputTypes()[0], Object.class);

            result = getNearestTransformerMatch(trans, criteria.getInputTypes()[0], criteria.getOutputType());
            if (result != null)
            {
                result = new TransformerChain(new Transformer[]{result, secondPass});
            }
        }

        if (result != null)
        {
            exactTransformerCache.put(criteria.getInputTypes()[0].getName() + criteria.getOutputType().getName(), result);
        }
        return result;
    }

    protected Transformer getNearestTransformerMatch(List trans, Class input, Class output) throws ResolverException
    {
        if (trans.size() > 1)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Comparing transformers for best match: input = " + input + " output = " + output + " Possible transformers = " + trans);
            }
            TransformerWeighting weighting = null;
            for (Iterator iterator = trans.iterator(); iterator.hasNext();)
            {
                Transformer transformer = (Transformer) iterator.next();
                TransformerWeighting current = new TransformerWeighting(input, output, transformer);
                if (weighting == null)
                {
                    weighting = current;
                }
                else
                {
                    int compare = current.compareTo(weighting);
                    if (compare == 1)
                    {
                        weighting = current;
                    }
                    else if (compare == 0)
                    {
                        //We may have two transformers that are exactly the same, in which case we can use either i.e. use the current
                        if (!weighting.getTransformer().getClass().equals(current.getTransformer().getClass()))
                        {
                            throw new ResolverException(CoreMessages.transformHasMultipleMatches(input, output,
                                    current.getTransformer(), weighting.getTransformer()));
                        }
                    }
                }
            }
            return weighting.getTransformer();
        }
        else if (trans.size() == 0)
        {
            return null;
        }
        else
        {
            return (Transformer) trans.get(0);
        }
    }

    public void dispose()
    {
        exactTransformerCache.clear();
    }

    public void transformerChange(Transformer transformer, RegistryAction registryAction)
    {
        if (transformer instanceof DiscoverableTransformer)
        {
            exactTransformerCache.clear();
        }
    }
}
