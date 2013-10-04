/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.model.resolvers;

import org.mule.api.MuleRuntimeException;
import org.mule.api.model.EntryPointResolver;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.ClassUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * An {@link org.mule.api.model.EntryPointResolverSet} that mimics the behaviour of the Mule 1.x
 * DynamicEntryPointResolver.
 * <b>NOTE:</b> Since 3.0 this legacy entry point resolver will always invoked after message
 * transformation  and not before.
 */
public class LegacyEntryPointResolverSet extends DefaultEntryPointResolverSet
{
    private static final String ANNOTATED_ENTRYPOINT_RESOLVER_CLASS = "org.mule.impl.model.resolvers.AnnotatedEntryPointResolver";
    private static final Log logger = LogFactory.getLog(LegacyEntryPointResolverSet.class);

    public LegacyEntryPointResolverSet()
    {
        addAnnotatedEntryPointResolver();
        addEntryPointResolver(new MethodHeaderPropertyEntryPointResolver());
        addEntryPointResolver(new CallableEntryPointResolver());

        ReflectionEntryPointResolver reflectionResolver = new ReflectionEntryPointResolver();
        //In Mule 1.x you could call setXX methods as service methods by default
        reflectionResolver.removeIgnoredMethod("set*");
        addEntryPointResolver(reflectionResolver);
    }

    protected void addAnnotatedEntryPointResolver()
    {
        //Annotations support is not part of Mule core, but we want default handling for annotations we have
        //work-arounds like this to avoid importing annotation classes
        //See MULE-4962 for details
        try
        {
            Class<? extends EntryPointResolver> annotatedEntrypointResolver =
                    ClassUtils.loadClass(ANNOTATED_ENTRYPOINT_RESOLVER_CLASS, getClass(), EntryPointResolver.class);
            addEntryPointResolver(ClassUtils.instanciateClass(annotatedEntrypointResolver, ClassUtils.NO_ARGS));
        }
        catch (ClassNotFoundException e)
        {
            if(logger.isDebugEnabled())
            {
                logger.warn("Mule annotations module is not on your classpath, annotations cannot be used on components");
            }
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(CoreMessages.cannotLoadFromClasspath(ANNOTATED_ENTRYPOINT_RESOLVER_CLASS));
        }
    }
}
