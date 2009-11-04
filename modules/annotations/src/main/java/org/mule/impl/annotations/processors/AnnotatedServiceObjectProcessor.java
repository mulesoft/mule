/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.annotations.processors;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.model.Model;
import org.mule.api.registry.PreInitProcessor;
import org.mule.config.annotations.Service;
import org.mule.impl.annotations.AnnotatedServiceBuilder;
import org.mule.impl.model.resolvers.AnnotatedEntryPointResolver;
import org.mule.model.resolvers.DefaultEntryPointResolverSet;
import org.mule.model.seda.SedaModel;

import java.util.Collection;
import java.util.Iterator;

/**
 * This object processor allows users to register annotated services directly to the registry
 * and have them configured correctly.
 * It will look for a non-system {@link org.mule.api.model.Model} registered with the Registry.
 * If one is not found a default  SEDA Model will be created
 * Finally, the processor will register the service with the Registry and return null.
 */
public class AnnotatedServiceObjectProcessor implements PreInitProcessor, MuleContextAware
{
    protected MuleContext context;

    public void setMuleContext(MuleContext context)
    {
        this.context = context;
    }

    public Object process(Object object)
    {
        if (object == null)
        {
            return object;
        }

        if (object.getClass().isAnnotationPresent(Service.class))
        {
            Model model = getOrCreateModel();
            org.mule.api.service.Service service = null;
            try
            {
                AnnotatedServiceBuilder builder = new AnnotatedServiceBuilder(context);
                builder.setModel(model);
                service = builder.createService(object);
            }
            catch (MuleException e)
            {
                throw new RuntimeException(e);
            }

            return service;
        }
        return object;
    }

    protected Model getOrCreateModel()
    {
        DefaultEntryPointResolverSet resolverSet = new DefaultEntryPointResolverSet();
        AnnotatedEntryPointResolver resolver = new AnnotatedEntryPointResolver();
        resolver.setMuleContext(context);
        resolverSet.addEntryPointResolver(resolver);

        Model model = null;
        try
        {
            Collection<Model> models = context.getRegistry().lookupObjects(Model.class);
            for (Iterator<Model> iterator = models.iterator(); iterator.hasNext();)
            {
                Model m = iterator.next();
                if (!m.getName().equals(MuleProperties.OBJECT_SYSTEM_MODEL))
                {
                    model = m;
                    model.setEntryPointResolverSet(resolverSet);
                    break;
                }
            }
            if (model == null)
            {
                //Create a new Model and add the Annotations EPR to the list
                model = createModel();
                model.setEntryPointResolverSet(resolverSet);
                context.getRegistry().registerModel(model);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        return model;

    }

    protected Model createModel()
    {
        return new SedaModel();
    }
}
