/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal;

import org.mule.extension.introspection.declaration.Describer;
import org.mule.module.extension.DescriberSupport;

/**
 * A {@link Describer} wich publishes the {@link ValidationExtension}
 *
 * @since 3.7.0
 */
public final class ValidationExtensionDescriber extends DescriberSupport
{

    public ValidationExtensionDescriber()
    {
        super(ValidationExtension.class);
    }
}
