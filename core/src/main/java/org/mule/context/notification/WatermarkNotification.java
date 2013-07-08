package org.mule.context.notification;

import org.mule.api.AnnotatedObject;
import org.mule.api.MuleEvent;
import org.mule.api.context.notification.BlockingServerEvent;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.processor.MessageProcessor;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

/**
 * <p>
 * Notification for Watermark. I contains the retrieve and response message with the watermark key and value.
 * </p>
 *
 * @since 3.5.0
 */
public class WatermarkNotification extends ServerNotification implements BlockingServerEvent
{

    public static final int WATERMARK_RETRIEVED = 130000;
    public static final int WATERMARK_STORED = WATERMARK_RETRIEVED + 1;

    static
    {
        registerAction("Watermark Retrieved", WatermarkNotification.WATERMARK_RETRIEVED);
        registerAction("Watermark Stored", WatermarkNotification.WATERMARK_STORED);
    }

    private static final long serialVersionUID = 1L;

    /**
     * The Object that fires the notification
     */
    protected final AnnotatedObject processor;

    /**
     * The name of the notification as an identifier
     */
    protected final String name;
    private String evaluatedVariable;
    private Serializable watermarkValue;


    public WatermarkNotification(final MuleEvent source, final AnnotatedObject processor,
                                 final String name, int action,
                                 String evaluatedVariable, Serializable watermarkValue)
    {
        super(source, action);

        this.processor = processor;
        this.name = name;
        this.evaluatedVariable = evaluatedVariable;
        this.watermarkValue = watermarkValue;
    }

    @Override
    public final MuleEvent getSource()
    {
        return (MuleEvent) super.getSource();
    }

    public final AnnotatedObject getAnnotatedGenerator()
    {
        return this.processor;
    }

    public final String getName()
    {
        return this.name;
    }

    public String getEvaluatedVariable()
    {
        return evaluatedVariable;
    }

    public Serializable getWatermarkValue()
    {
        return watermarkValue;
    }
}
