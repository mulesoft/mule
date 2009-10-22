package org.mule.config.annotations.endpoints;

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
     * The short protocol name for the connector. Where the annotation is general purpose, a string ID can be used such as
     * 'outbound' or 'reply'.
     *
     * @return the resource name for the connector
     */
    String identifer();

    /**
     * The endpoint type. {@link ChannelType.Inbound} is used to indicate that
     * the endpoint annotation is used for receiving messages. {@link ChannelType.Outbound} indicates that the endpoint
     * will be used for dispatching events.
     *
     * @return
     */
    ChannelType type();
}