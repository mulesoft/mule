/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static org.mule.runtime.core.util.Preconditions.checkArgument;

import org.mule.runtime.core.util.ArrayUtils;

import com.google.common.collect.ImmutableList;

import java.util.Collection;
import java.util.List;

/**
 * Implementation of {@link OptionalObjectsController} which groups
 * a list of controllers and executes every operation on all of them.
 *
 * @since 3.7.0
 */
public class CompositeOptionalObjectsController implements OptionalObjectsController
{

    private final List<OptionalObjectsController> controllers;

    public CompositeOptionalObjectsController(OptionalObjectsController... controllers)
    {
        checkArgument(!ArrayUtils.isEmpty(controllers), "cannot compose empty controllers list");
        this.controllers = ImmutableList.copyOf(controllers);
    }

    @Override
    public void registerOptionalKey(String key)
    {
        for (OptionalObjectsController controller : controllers)
        {
            controller.registerOptionalKey(key);
        }
    }

    @Override
    public void discardOptionalObject(String key)
    {
        for (OptionalObjectsController controller : controllers)
        {
            controller.discardOptionalObject(key);
        }
    }

    @Override
    public boolean isOptional(String key)
    {
        for (OptionalObjectsController controller : controllers)
        {
            if (controller.isOptional(key))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isDiscarded(String key)
    {
        for (OptionalObjectsController controller : controllers)
        {
            if (controller.isDiscarded(key))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public Object getDiscardedObjectPlaceholder()
    {
        for (OptionalObjectsController controller : controllers)
        {
            Object placeHolder = controller.getDiscardedObjectPlaceholder();
            if (placeHolder != null)
            {
                return placeHolder;
            }
        }

        return null;
    }

    @Override
    public Collection<String> getAllOptionalKeys()
    {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        for (OptionalObjectsController controller : controllers)
        {
            builder.addAll(controller.getAllOptionalKeys());
        }

        return builder.build();
    }
}
