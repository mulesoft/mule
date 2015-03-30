/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension;

import org.mule.extension.introspection.Describer;
import org.mule.extension.introspection.declaration.Construct;
import org.mule.module.extension.internal.introspection.AnnotationsBasedDescriber;

/**
 * Utility class for easily building a {@link Describer}
 * that is discoverable through SPI for an extension defined
 * through annotations without coupling
 * to mule's mechanism to actually parse those annotations.
 * <p/>
 * Users simply extend this class and add a default constructor
 * which delegates into {@link #DescriberSupport(Class)}.
 *
 * @since 3.7.0
 */
public abstract class DescriberSupport implements Describer
{

    private final Describer delegate;

    protected DescriberSupport(Class<?> extensionType)
    {
        delegate = new AnnotationsBasedDescriber(extensionType);
    }

    @Override
    public final Construct describe()
    {
        return delegate.describe();
    }
}
