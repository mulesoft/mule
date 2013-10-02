/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.annotations.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint annotation and provides the connector protocol and type.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface Channel
{
    /**
     * The short protocol name for the connector. Where the annotation is general
     * purpose, a string ID can be used such as 'outbound' or 'reply'.
     *
     * @return the resource name for the connector
     */
    String identifer();

    /**
     * The endpoint type. <code>ChannelType.Inbound</code> is used to indicate that
     * the endpoint annotation is used for receiving messages.
     * <code>ChannelType.Outbound</code> indicates that the endpoint will be used for
     * dispatching events.
     */
    ChannelType type();
}
