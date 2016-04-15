/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.grizzly;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.filter.FilterContext;
import com.ning.http.client.filter.FilterException;
import com.ning.http.client.filter.RequestFilter;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link com.ning.http.client.filter.RequestFilter} that throttles requests and blocks when the number of permits
 * is reached, waiting for the response to arrive before executing the next request.
 *
 * This is based on {@code com.ning.http.client.extra.ThrottleRequestFilter} from Async Http Client, but uses the
 * request timeout from each request.
 */
public class CustomTimeoutThrottleRequestFilter implements RequestFilter
{

    private final static Logger logger = LoggerFactory.getLogger(CustomTimeoutThrottleRequestFilter.class);
    private final Semaphore available;

    public CustomTimeoutThrottleRequestFilter(int maxConnections)
    {
        available = new Semaphore(maxConnections, true);
    }

    @Override
    public FilterContext filter(FilterContext ctx) throws FilterException
    {
        try
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Current Throttling Status {}", available.availablePermits());
            }
            if (!available.tryAcquire(ctx.getRequest().getRequestTimeout(), MILLISECONDS))
            {
                throw new FilterException(
                        String.format("No slot available for processing Request %s with AsyncHandler %s",
                                      ctx.getRequest(), ctx.getAsyncHandler()));
            }
        }
        catch (InterruptedException e)
        {
            throw new FilterException(
                    String.format("Interrupted Request %s with AsyncHandler %s", ctx.getRequest(), ctx.getAsyncHandler()));
        }

        return new FilterContext.FilterContextBuilder(ctx).asyncHandler(new AsyncHandlerWrapper(ctx.getAsyncHandler())).build();
    }

    private class AsyncHandlerWrapper<T> implements AsyncHandler<T>
    {

        private final AsyncHandler<T> asyncHandler;
        private final AtomicBoolean complete = new AtomicBoolean(false);

        public AsyncHandlerWrapper(AsyncHandler<T> asyncHandler)
        {
            this.asyncHandler = asyncHandler;
        }

        private void complete()
        {
            if (complete.compareAndSet(false, true))
            {
                available.release();
            }
            if (logger.isDebugEnabled())
            {
                logger.debug("Current Throttling Status after onThrowable {}", available.availablePermits());
            }
        }

        @Override
        public void onThrowable(Throwable t)
        {
            try
            {
                asyncHandler.onThrowable(t);
            }
            finally
            {
                complete();
            }
        }

        @Override
        public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception
        {
            return asyncHandler.onBodyPartReceived(bodyPart);
        }

        @Override
        public STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception
        {
            return asyncHandler.onStatusReceived(responseStatus);
        }

        @Override
        public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception
        {
            return asyncHandler.onHeadersReceived(headers);
        }

        @Override
        public T onCompleted() throws Exception
        {
            try
            {
                return asyncHandler.onCompleted();
            }
            finally
            {
                complete();
            }
        }
    }
}
