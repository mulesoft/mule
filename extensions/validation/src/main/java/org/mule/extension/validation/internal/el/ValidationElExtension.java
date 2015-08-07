/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.internal.el;

import org.mule.api.MuleEvent;
import org.mule.api.el.ExpressionLanguageContext;
import org.mule.api.el.ExpressionLanguageExtension;

/**
 * A {@link ExpressionLanguageExtension} which adds a
 * {@link ValidatorElContext} to the {@link ExpressionLanguageContext}
 * as a variable named {@code validator}.
 * <p/>
 * The {@code validator} variable is actually an alias which
 * creates a new {@link ValidatorElContext} each time a
 * {@link ExpressionLanguageContext} is configured. This is necessary
 * because the {@link ValidatorElContext} needs to receive the current
 * {@link MuleEvent} in its constructor.
 *
 * @since 3.7.0
 */
public final class ValidationElExtension implements ExpressionLanguageExtension
{

    /**
     * Registers an alias for a new {@link ValidatorElContext}
     * under the name {@code validator}
     *
     * @param context a new {@link ExpressionLanguageContext} which is being configured
     */
    @Override
    public void configureContext(ExpressionLanguageContext context)
    {
        context.addFinalVariable("validator", new ValidatorElContext());
    }
}
