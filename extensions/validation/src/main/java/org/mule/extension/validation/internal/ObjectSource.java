/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.util.Preconditions.checkArgument;
import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.registry.MuleRegistry;
import org.mule.extension.annotations.Parameter;
import org.mule.extension.annotations.param.Optional;
import org.mule.util.ClassUtils;

import java.lang.reflect.Constructor;

import org.apache.commons.lang.StringUtils;

/**
 * A factory object for providing instances by either referencing their classname
 * (through the {@link #type} attribute, or a {@link MuleRegistry} reference (through the
 * {@link #ref} one.
 * <p/>
 * When the {@link #type} attribute is used to reference a type, then a new instance is returned each time
 * that {@link #getObject(MuleContext)} is invoked. That type is also expected to have a public default {@link Constructor}.
 * <p/>
 * When a {@link #ref} is provided,
 * then that value is searched by using the {@link MuleRegistry#get(String)}. Notice however that the reference will
 * be re fetched each time that {@link #getObject(MuleContext)} is invoked
 * <p/>
 * The {@link #type} and {@link #ref} attributes are mutually exclusive. A {@link IllegalArgumentException} is thrown
 * if both are set by the time {@link #getObject(MuleContext)} is invoked. The same exception is also thrown
 * if none of them are.
 * <p/>
 * Instances of this class are to be considered thread-safe and reusable.
 *
 * @param <T> the type of the object to be returned
 * @since 3.7.0
 */
public class ObjectSource<T>
{

    @Parameter(alias = "class")
    @Optional
    private String type;

    @Parameter
    @Optional
    private String ref;

    public ObjectSource()
    {
    }

    public ObjectSource(String type, String ref)
    {
        this.type = type;
        this.ref = ref;
    }

    public final T getObject(MuleContext muleContext)
    {
        boolean hasType = !StringUtils.isBlank(type);
        boolean hasRef = !StringUtils.isBlank(ref);

        checkArgument(!(hasType && hasRef), "type and ref attributes are mutually exclusive. Please provide only one of them");
        checkArgument(hasType ^ hasRef, "One of class or ref attributes are required. Please provide one of them");

        if (hasRef)
        {
            return doGetByRef(muleContext);
        }
        else
        {
            return doGetByClassName();
        }
    }

    protected T doGetByClassName()
    {
        Class<T> objectClass;
        try
        {
            objectClass = (Class<T>) ClassUtils.loadClass(type, getClass());
        }
        catch (ClassNotFoundException e)
        {
            throw new IllegalArgumentException("Could not find class " + type, e);
        }

        try
        {
            return objectClass.newInstance();
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(createStaticMessage("Could not create instance of " + type), e);
        }
    }

    protected T doGetByRef(MuleContext muleContext)
    {
        return muleContext.getRegistry().get(ref);
    }

    public final String getType()
    {
        return type;
    }

    public final String getRef()
    {
        return ref;
    }
}
