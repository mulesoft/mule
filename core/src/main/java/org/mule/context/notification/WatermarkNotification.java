package org.mule.context.notification;

import org.mule.api.MuleEvent;
import org.mule.api.context.notification.BlockingServerEvent;
import org.mule.api.processor.MessageProcessor;

import java.util.Collections;
import java.util.Map;

/**
 * Custom Notification with metadata included. The metadata is useful to represent status of the event and custom
 * properties.
 */
public class WatermarkNotification implements BlockingServerEvent
{
    static
    {
        registerAction("custom event", WatermarkNotification.CUSTOM_EVENT_EVENT_ACTION);
    }

    public static final int CUSTOM_EVENT_EVENT_ACTION = 120000;

    private static final long serialVersionUID = 1L;

    /**
     * The message processor that fires the notification
     */
    protected final MessageProcessor processor;

    /**
     * The name of the notification as an identifier
     */
    protected final String name;

    /**
     * The notification metadata
     */
    protected final Map<String, String> metaData;

    public WatermarkNotification(final MuleEvent source, final MessageProcessor processor,
                                 final String name, final Map<String, String> metaData,
                                 int action)
    {
        super(source, action);

        this.processor = processor;
        this.name = name;
        this.metaData = metaData;
    }

    public WatermarkNotification(final MuleEvent source, final MessageProcessor processor,
                                 final String name, final Map<String, String> metaData)
    {
        super(source, WatermarkNotification.CUSTOM_EVENT_EVENT_ACTION);

        this.processor = processor;
        this.name = name;
        this.metaData = metaData;
    }

    @Override
    public final MuleEvent getSource()
    {
        return (MuleEvent) super.getSource();
    }

    public final MessageProcessor getProcessor()
    {
        return this.processor;
    }

    public final String getName()
    {
        return this.name;
    }

    public final Map<String, String> getMetaData()
    {
        return Collections.<String, String>unmodifiableMap(this.metaData);
    }
}
