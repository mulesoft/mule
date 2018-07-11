/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.http.internal.request.grizzly;

import static org.eclipse.jetty.http.HttpMethod.POST;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;

import org.junit.Test;
import org.mockito.Matchers;
import org.mule.module.http.internal.domain.InputStreamHttpEntity;
import org.mule.module.http.internal.domain.request.HttpRequest;

public class SafeHeadersRetrievalTestCase extends AbstractHttpClientTestCase
{

    private static final int NUMBER_OF_REQUESTS = 100;

    /**
     * MULE-15326: this test verifies that in case a tracing tool retrieves the response's headers no Concurrent Exception
     * is raised. There was a bug in grizzly which resulted in this. If no exception is raised, the test succeeds.
     * 
     * @throws Exception in case an error arises
     */
    @Test
    public void verifyAccessToHeadersIsSafe() throws Exception
    {
        InputStream requestInputStream = mock(InputStream.class);
        when(requestInputStream.read(Matchers.<byte[]> any())).thenReturn(1).thenReturn(-1);
        httpClient.setMustAsyncRetrieveHeadersInResponse(true);
        HttpRequest request = createRequest(new InputStreamHttpEntity(requestInputStream), POST.name());
        for (int i = 0; i < NUMBER_OF_REQUESTS; i++)
        {
            httpClient.send(request, TIMEOUT, FOLLOW_REDIRECTS, null);
        }
    }


}
