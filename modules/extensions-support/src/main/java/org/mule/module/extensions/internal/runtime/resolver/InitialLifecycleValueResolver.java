/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.runtime.resolver;

import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.api.lifecycle.Startable;

/**
 * A {@link ValueResolver} wrapper which when needed applies the initial lifecycle
 * to each returned object.
 * <p/>
 * On a more formal definition, it will call the following methods (in given order)
 * when the returned object implements the referenced interfaces:
 * <ul>
 * <li>{@link org.mule.api.context.MuleContextAware#setMuleContext(MuleContext)}</li>
 * <li>{@link org.mule.api.lifecycle.Initialisable#initialise()}</li>
 * <li>{@link org.mule.api.lifecycle.Startable#start()}</li>
 * </ul>
 *
 * @since 3.7.0
 */
public class InitialLifecycleValueResolver<T> extends BaseValueResolverWrapper<T>
{

    /**
     * Creates a new instance
     *
     * @param delegate    the delegate {@link ValueResolver}
     * @param muleContext a not {code null} {@link ValueResolver}
     * @throws IllegalArgumentException if either {@code delegate} or {@code muleContext} are {@code null}
     */
    public InitialLifecycleValueResolver(ValueResolver<T> delegate, MuleContext muleContext)
    {
        super(delegate);
        checkArgument(muleContext != null, "muleContext cannot be null");
        setMuleContext(muleContext);
    }

    /**
     * Resolves the value by delegating into {@link #delegate#resolve(MuleEvent)}.
     * Before the value is returned, it sets the {@link MuleContext} and applies lifecycle
     * as described in the class' comments
     *
     * @param event a {@link MuleEvent}
     * @return the resolved value
     * @throws Exception
     */
    @Override
    public T resolve(MuleEvent event) throws MuleException
    {
        T resolved = delegate.resolve(event);

        LifecycleUtils.initialiseIfNeeded(resolved, muleContext);

        if (resolved instanceof Startable)
        {
            ((Startable) resolved).start();
        }

        return resolved;
    }
}
