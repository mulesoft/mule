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

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.Matchers;
import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.request.HttpRequest;

public class InputStreamSendHttpClientTestCase extends AbstractHttpClientTestCase
{

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
        InputStream requestInputStream = mock(InputStream.class);
        when(requestInputStream.read(Matchers.<byte[]>any())).thenReturn(1).thenReturn(-1);
        HttpRequest request = createRequest(new InputStreamHttpEntity(requestInputStream), POST.name());
        httpClient.send(request, TIMEOUT, FOLLOW_REDIRECTS, null);
        verify(requestInputStream).close();
    }

}
