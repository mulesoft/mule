/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import static org.mule.module.extension.internal.util.IntrospectionUtils.checkInstantiable;
import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.MessageFactory;
import org.mule.util.ClassUtils;

/**
 * Default implementation of {@link ObjectBuilder} which
 * creates instances through a provided {@link Class}.
 *
 * @since 3.7.0
 */
public class DefaultObjectBuilder<T> extends BaseObjectBuilder<T>
{

    private final Class<T> prototypeClass;

    /**
     * Creates a new instance that will build instances of {@code prototypeClass}.
     *
     * @param prototypeClass a {@link Class} which needs to have a public defualt constructor
     */
    public DefaultObjectBuilder(Class<T> prototypeClass)
    {
        checkInstantiable(prototypeClass);
        this.prototypeClass = prototypeClass;
    }

    /**
     * Creates a new instance by calling the default constructor on {@link #prototypeClass}
     * {@inheritDoc}
     */
    @Override
    protected T instantiateObject()
    {
        try
        {
            return ClassUtils.instanciateClass(prototypeClass);
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(MessageFactory.createStaticMessage("Could not create instance of " + prototypeClass), e);
        }
    }
}
