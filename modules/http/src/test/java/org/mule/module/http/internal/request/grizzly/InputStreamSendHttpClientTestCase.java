/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.internal.request.grizzly;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.eclipse.jetty.http.HttpMethod.GET;
import static org.eclipse.jetty.http.HttpMethod.POST;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Matchers;
import org.mule.api.MuleException;
import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.request.HttpRequest;
import org.mule.module.http.internal.request.HttpClientConfiguration;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.transport.ssl.api.TlsContextFactory;
import org.mule.transport.ssl.api.TlsContextFactoryBuilder;

@RunWith(Parameterized.class)
public class InputStreamSendHttpClientTestCase extends AbstractHttpClientTestCase
{

    private static final int POLL_TIMEOUT = 2000;
    private static final int POLL_DELAY = 200;

    private final PollingProber pollingProber = new PollingProber(POLL_TIMEOUT, POLL_DELAY);

    private boolean streaming;

    public InputStreamSendHttpClientTestCase(boolean streaming)
    {
        this.streaming = streaming;

    }

    @Parameters
    public static List<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                                             {true},
                                             {false}
        });
    }

    @Test
    public void retrieveContentsFromInputStream() throws Exception
    {
        InputStream responseStream = null;

        HttpRequest request = createRequest(null, GET.name());
        responseStream = httpClient.sendAndReceiveInputStream(request, TIMEOUT, FOLLOW_REDIRECTS, null);

        String response = IOUtils.toString(responseStream, UTF_8.name());

        assertThat(EXPECTED_RESULT, equalTo(response));
    }


    @Test
    public void testInputStreamIsClosed() throws Exception
    {
        final InputStream requestInputStream = mock(InputStream.class);
        when(requestInputStream.read(Matchers.<byte[]> any())).thenReturn(1).thenReturn(-1);
        HttpRequest request = createRequest(new InputStreamHttpEntity(requestInputStream), POST.name());
        httpClient.send(request, TIMEOUT, FOLLOW_REDIRECTS, null);

        // In case of deferring streaming, the request input stream is
        // closed asynchronously.
        pollingProber.check(new JUnitProbe()
        {
            @Override
            protected boolean test() throws Exception
            {
                verify(requestInputStream).close();
                return true;
            }
        });

    }

    @Override
    protected void createClient() throws MuleException
    {
        TlsContextFactory defaultTlsContextFactory = new TlsContextFactoryBuilder(muleContext).buildDefault();
        HttpClientConfiguration configuration = new HttpClientConfiguration.Builder().setUsePersistentConnections(true)
                                                                                     .setDefaultTlsContextFactory(defaultTlsContextFactory)
                                                                                     .setMaxConnections(1)
                                                                                     .setStreaming(streaming)
                                                                                     .setConnectionIdleTimeout(-1)
                                                                                     .setResponseBufferSize(1000)
                                                                                     .build();
        httpClient = new TestGrizzlyHttpClient(configuration);
        httpClient.start();
    }

}
