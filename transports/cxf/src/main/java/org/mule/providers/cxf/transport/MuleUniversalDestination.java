
package org.mule.providers.cxf.transport;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.logging.Logger;

import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractConduit;
import org.apache.cxf.transport.AbstractDestination;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

public class MuleUniversalDestination extends AbstractDestination
{
    public static final String RESPONSE_OBSERVER = "mule.destination.response.observer";

    private static final Logger LOGGER = Logger.getLogger(MuleUniversalDestination.class.getName());
    private MuleUniversalTransport transport;

    public MuleUniversalDestination(MuleUniversalTransport transport,
                                    EndpointReferenceType ref,
                                    EndpointInfo ei)
    {
        super(ref, ei);
        this.transport = transport;

        // String uri = getAddress().getAddress().getValue().toString();

        // TODO - support decoupled endpoints
        // UMOEndpoint ep;
        // try {
        // System.out.println("creating endpoint " + uri);
        // ep = MuleEndpoint.getOrCreateEndpointForUri("cxf:" + uri,
        // UMOEndpoint.ENDPOINT_TYPE_RECEIVER);
        // } catch (UMOException e) {
        // throw new RuntimeException(e);
        // }
        // ep.setConnector(transport.getConnector());
    }

    @Override
    protected Conduit getInbuiltBackChannel(Message inMessage)
    {
        return new ResponseConduit(null, (MessageObserver) inMessage.get(RESPONSE_OBSERVER));
    }

    @Override
    protected Logger getLogger()
    {
        return LOGGER;
    }

    @Override
    public void shutdown()
    {
        transport.remove(this);

        super.shutdown();
    }

    @Override
    protected boolean markPartialResponse(Message partialResponse, EndpointReferenceType decoupledTarget)
    {
        // setup the outbound message to for 202 Accepted
        partialResponse.put(Message.RESPONSE_CODE, HttpURLConnection.HTTP_ACCEPTED);
        partialResponse.getExchange().put(EndpointReferenceType.class, decoupledTarget);
        return true;
    }

    /**
     * @return the associated conduit initiator, or null if decoupled mode not
     *         supported.
     */
    @Override
    protected ConduitInitiator getConduitInitiator()
    {
        return transport;
    }

    public class ResponseConduit extends AbstractConduit
    {

        private MessageObserver observer;

        public ResponseConduit(EndpointReferenceType arg0, MessageObserver observer)
        {
            super(arg0);
            this.observer = observer;
        }

        public void prepare(Message message) throws IOException
        {
            CachedOutputStream stream = new CachedOutputStream();
            message.setContent(OutputStream.class, stream);
            // keep it around in case someone wants to replace the OutputStream along
            // the way
            message.setContent(CachedOutputStream.class, stream);
        }

        @Override
        public void close(Message message) throws IOException
        {
            message.getContent(OutputStream.class).close();
            observer.onMessage(message);
        }

        @Override
        protected Logger getLogger()
        {
            return LOGGER;
        }

    }
}
