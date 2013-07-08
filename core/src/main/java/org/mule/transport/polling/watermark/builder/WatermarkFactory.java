package org.mule.transport.polling.watermark.builder;

import org.mule.api.AnnotatedObject;
import org.mule.api.construct.FlowConstruct;
import org.mule.transport.polling.watermark.Watermark;

/**
 * <p>
 * Factory of the {@link Watermark} object created by spring.
 * </p>
 *
 * @since 3.5.0
 */
public interface WatermarkFactory extends AnnotatedObject
{

    /**
     * <p>
     * The {@link Watermark} is related to the flow of the poll so it is needed for its creation
     * </p>
     *
     * @param flowConstruct The Flow construct that contains the Poll element with {@link Watermark}
     * @return The created {@link Watermark}
     * @throws Exception In case the {@link Watermark} was created for a non Synchronous {@link FlowConstruct}
     */
    Watermark createFor(FlowConstruct flowConstruct) throws Exception;
}
