
package org.mule.module.ws.construct;

import java.net.InetAddress;

import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transport.Connector;
import org.mule.api.transport.MessageRequester;
import org.mule.construct.AbstractFlowConstruct;
import org.mule.construct.AbstractFlowConstuctTestCase;
import org.mule.tck.MuleTestUtils;
import org.mule.transport.AbstractMessageRequester;
import org.mule.transport.AbstractMessageRequesterFactory;

public abstract class AbstractWSProxyTestCase extends AbstractFlowConstuctTestCase
{
    protected Connector testConnector;
    private WSProxy wsProxy;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        final OutboundEndpoint testOutboundEndpoint = MuleTestUtils.getTestOutboundEndpoint(
            MessageExchangePattern.REQUEST_RESPONSE, muleContext);
        testConnector = testOutboundEndpoint.getConnector();
        muleContext.getRegistry().registerConnector(testConnector);
        testConnector.start();

        wsProxy = newWSProxy(testOutboundEndpoint);
    }

    protected abstract WSProxy newWSProxy(OutboundEndpoint testOutboundEndpoint) throws Exception;

    @Override
    protected AbstractFlowConstruct getFlowConstruct() throws Exception
    {
        return wsProxy;
    }

    private void startWsProxy() throws InitialisationException, MuleException
    {
        wsProxy.initialise();
        wsProxy.start();
    }

    public void testProcessNonHttpRequest() throws Exception
    {
        startWsProxy();

        final MuleEvent response = directInboundMessageSource.process(MuleTestUtils.getTestInboundEvent(
            "hello", muleContext));

        assertEquals("hello", response.getMessageAsString());
    }

    public void testProcessHttpWsdlRequest() throws Exception
    {
        startWsProxy();

        testConnector.setRequesterFactory(new AbstractMessageRequesterFactory()
        {
            @Override
            public MessageRequester create(InboundEndpoint endpoint) throws MuleException
            {
                return new AbstractMessageRequester(endpoint)
                {
                    @Override
                    protected MuleMessage doRequest(long timeout) throws Exception
                    {
                        assertEquals("test://test?wsdl", endpoint.getEndpointURI().toString());
                        return new DefaultMuleMessage("fake_wsdl localhost", muleContext);
                    }
                };
            }
        });

        final MuleEvent event = MuleTestUtils.getTestInboundEvent("hello", muleContext);
        event.getMessage().setInboundProperty("http.request", "test://foo?WSDL");
        final MuleEvent response = directInboundMessageSource.process(event);

        assertEquals("fake_wsdl " + InetAddress.getLocalHost().getHostName(), response.getMessageAsString());
    }

    public void testProcessHttpServiceRequest() throws Exception
    {
        startWsProxy();
        final MuleEvent event = MuleTestUtils.getTestInboundEvent("hello", muleContext);
        event.getMessage().setInboundProperty("http.request", "http://foo");
        final MuleEvent response = directInboundMessageSource.process(event);

        assertEquals("hello", response.getMessageAsString());
    }
}
