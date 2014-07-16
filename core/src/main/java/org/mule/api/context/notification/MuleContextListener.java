/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.context.notification;

import org.mule.api.MuleContext;

/**
 *  Listens to events raised during the creation of a {@link MuleContext}
 */
public interface MuleContextListener
{

    /**
     * Notifies the creation of a {@link MuleContext} instance right before
     * the initialization.
     *
     * @param context created context
     */
    void onCreation(MuleContext context);

    /**
     * Notifies the initialization of a {@link MuleContext} instance right
     * before the configuration.
     *
     * @param context initialized context
     */
    void onInitialization(MuleContext context);

    /**
     * Notifies the configuration of a {@link MuleContext} instance, after this
     * notification, the context is ready to be used.
     *
     * @param context configured context
     */
    void onConfiguration(MuleContext context);
}
