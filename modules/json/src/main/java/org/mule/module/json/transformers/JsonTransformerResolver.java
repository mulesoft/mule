/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.transformers;

import org.apache.commons.collections.Predicate;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.mule.util.CollectionUtils;

import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A {@link org.mule.api.registry.TransformerResolver} implementation used to discover whether the current transform
 * requests requires Json mashaling. The resolver will scan the source and return type for Jackson (http://jackson.codehaus.org) Json annotations and will configure
 * a JSON transformer accordingly.  The transformer is cached and will be used for any subsequent requests.
 *
 * The {@link org.codehaus.jackson.map.ObjectMapper} instance needed for the transform can be discovered from the registry, this means one can be
 * pre-configured in Spring or Guice.  If there is no pre-configured {@link org.codehaus.jackson.map.ObjectMapper} one will be created with the
 * annotated JSON class.  This context will cached with the transformer.
 *
 * @since 3.0
 */
public class JsonTransformerResolver implements TransformerResolver, MuleContextAware, Disposable
{
    public static final String JSON_MIME_TYPE = "application/json";
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(JsonTransformerResolver.class);
    
    private MuleContext muleContext;

    //We cache the the transformers, this will get cleared when the server shuts down
    private Map<String, Transformer> transformerCache = new WeakHashMap<String, Transformer>();

    private JsonMapperResolver resolver;

    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    public Transformer resolve(DataType<?> source, DataType<?> result) throws ResolverException
    {
        //Check the cache
        Transformer t = transformerCache.get(source.toString() + result.toString());

        if (t != null)
        {
            return t;
        }

        try
        {
            ObjectMapper mapper = getMapperResolver().resolve(ObjectMapper.class, source, result, muleContext);

            if(mapper==null)
            {
                return null;
            }
            boolean marshal;
            Class<?> annotatedType;

            //Check the class caches before we start scanning classes
            if (getMapperResolver().getMatchingClasses().contains(result.getType()))
            {
                annotatedType = result.getType();
                //Set the correct mime type on the raw type
                source = source.cloneDataType();
                source.setMimeType(JSON_MIME_TYPE);
                marshal = false;
            }
            else if (getMapperResolver().getMatchingClasses().contains(source.getType()))
            {
                annotatedType = source.getType();
                //Set the correct mime type on the raw type
                result = result.cloneDataType();
                result.setMimeType(JSON_MIME_TYPE);
                marshal = true;
            }
            else
            {
                return null;
            }


            //At this point we know we are dealing with Json, now lets check the registry to see if there is an exact
            //transformer that matches our criteria
            Collection<Transformer> ts = muleContext.getRegistry().lookupTransformers(source, result);
            //ObjectToString continues to cause pain to auto transforms, here
            //we check explicitly since we want to generate a Json transformer if
            //one does not already exist in the context
            ts = CollectionUtils.select(ts, new Predicate()
            {
                @Override
                public boolean evaluate(Object object)
                {
                    if (object instanceof ObjectToString)
                    {
                        return false;
                    }
                    return true;
                }
            });

            if (ts.size() == 1)
            {
                t = ts.iterator().next();
            }
            else if (marshal)
            {
                ObjectToJson otj = new ObjectToJson();
                otj.setSourceClass(annotatedType);
                otj.setReturnDataType(result);
                otj.setMapper(mapper);
                muleContext.getRegistry().applyProcessorsAndLifecycle(otj);
                t = otj;
            }
            else
            {
                JsonToObject jto = new JsonToObject();
                jto.setReturnDataType(result);
                jto.setMapper(mapper);
                muleContext.getRegistry().applyProcessorsAndLifecycle(jto);
                t = jto;
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

    protected JsonMapperResolver getMapperResolver() throws ResolverException
    {
        if(resolver==null)
        {
            try
            {
                resolver = muleContext.getRegistry().lookupObject(JsonMapperResolver.class);
            }
            catch (RegistrationException e)
            {
                throw new ResolverException(e.getI18nMessage(), e);
            }
        }
        return resolver;
    }
}
