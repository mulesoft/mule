/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
