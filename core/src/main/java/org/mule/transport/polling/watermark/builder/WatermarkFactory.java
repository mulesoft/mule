package org.mule.transport.polling.watermark.builder;

import org.mule.api.AnnotatedObject;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.transport.polling.watermark.Watermark;

public interface WatermarkFactory extends AnnotatedObject
{

    Watermark buildFor(FlowConstruct flowConstruct);
}
