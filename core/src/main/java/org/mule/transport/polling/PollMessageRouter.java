package org.mule.transport.polling;

import org.mule.DefaultMuleEvent;
import org.mule.MessageExchangePattern;
import org.mule.ResponseOutputStream;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.SessionHandler;
import org.mule.transport.RoutingMessageTemplate;

/**
 * <p>
 *     Implementation of {@link RoutingMessageTemplate} for poll elements.
 * </p>
 *
 * @since 3.5.0
 */
public class PollMessageRouter extends RoutingMessageTemplate
{


    private FlowConstruct flowConstruct;

    public PollMessageRouter(MessageProcessor listener,
                             MessageExchangePattern exchangePattern, SessionHandler sessionHandler, FlowConstruct flowConstruct)
    {
        super(listener, exchangePattern, sessionHandler);
        this.flowConstruct = flowConstruct;
    }


    @Override
    protected void applyInboundTransformers(MuleEvent muleEvent) throws MuleException
    {
        // no inbound transformer for poll
    }

    @Override
    protected MuleEvent doCreateEvent(MuleMessage message, ResponseOutputStream ros, MuleSession session)
    {
        return new DefaultMuleEvent(message, exchangePattern, flowConstruct, session);
    }

    @Override
    protected void fireResponseNotification(MuleEvent resultEvent)
    {
        //TODO: SEND notification? if so, what type?
        // We are not sending response notification now
    }

    @Override
    protected void applyResponseTransformers(MuleEvent resultEvent) throws MuleException
    {
        // No response transformer for poll
    }


}
