/*
 * Copyright (c) 2012-2015 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.ning.http.client.providers.grizzly;

import com.ning.http.client.providers.grizzly.events.GracefulCloseEvent;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.Request;
import com.ning.http.client.uri.Uri;
import com.ning.http.client.ws.WebSocket;
import com.ning.http.util.AsyncHttpProviderUtils;
import java.io.IOException;
import org.glassfish.grizzly.CloseListener;
import org.glassfish.grizzly.CloseType;
import org.glassfish.grizzly.Closeable;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.attributes.Attribute;
import org.glassfish.grizzly.attributes.AttributeStorage;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.http.HttpContext;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.websockets.HandShake;
import org.glassfish.grizzly.websockets.ProtocolHandler;

/**
 *
 * @author Grizzly team
 *
 * This is a modified version from the original file from AHC.
 */
public final class HttpTransactionContext {
    private static final Attribute<HttpTransactionContext> REQUEST_STATE_ATTR =
            Grizzly.DEFAULT_ATTRIBUTE_BUILDER.createAttribute(HttpTransactionContext.class.getName());

    int redirectCount;
    final int maxRedirectCount;
    final boolean redirectsAllowed;
    final GrizzlyAsyncHttpProvider provider;

    private final Request ahcRequest;
    Uri requestUri;

    private final Connection connection;

    PayloadGenerator payloadGenerator;

    StatusHandler statusHandler;
    // StatusHandler invocation status
    StatusHandler.InvocationStatus invocationStatus =
            StatusHandler.InvocationStatus.CONTINUE;

    GrizzlyResponseFuture future;
    HttpResponsePacket responsePacket;
    GrizzlyResponseStatus responseStatus;


    Uri lastRedirectUri;
    long totalBodyWritten;
    AsyncHandler.STATE currentState;
    Uri wsRequestURI;
    boolean isWSRequest;
    HandShake handshake;
    ProtocolHandler protocolHandler;
    WebSocket webSocket;
    boolean establishingTunnel;

    // don't recycle the context, don't return associated connection to
    // the pool
    boolean isReuseConnection;


    private final CloseListener listener = new CloseListener<Closeable, CloseType>() {
        @Override
        public void onClosed(Closeable closeable, CloseType type) throws IOException {
            if (isGracefullyFinishResponseOnClose()) {
                // Connection was closed.
                // This event is fired only for responses, which don't have
                // associated transfer-encoding or content-length.
                // We have to complete such a request-response processing gracefully.
                final FilterChain fc = (FilterChain) connection.getProcessor();
                fc.fireEventUpstream(connection,
                                     new GracefulCloseEvent(HttpTransactionContext.this), null);
            } else if (CloseType.REMOTELY.equals(type)) {
                abort(AsyncHttpProviderUtils.REMOTELY_CLOSED_EXCEPTION);
            }
        }
    };

    // -------------------------------------------------------- Static methods
    static void bind(final HttpContext httpCtx,
                     final HttpTransactionContext httpTxContext) {
        httpCtx.getCloseable().addCloseListener(httpTxContext.listener);
        REQUEST_STATE_ATTR.set(httpCtx, httpTxContext);
    }

    static HttpTransactionContext cleanupTransaction(final HttpContext httpCtx) {
        final HttpTransactionContext httpTxContext = currentTransaction(httpCtx);
        if (httpTxContext != null) {
            httpCtx.getCloseable().removeCloseListener(httpTxContext.listener);
        }

        return httpTxContext;
    }

    static HttpTransactionContext currentTransaction(
            final HttpHeader httpHeader) {
        return currentTransaction(httpHeader.getProcessingState().getHttpContext());
    }

    static HttpTransactionContext currentTransaction(final AttributeStorage storage) {
        return REQUEST_STATE_ATTR.get(storage);
    }

    static HttpTransactionContext currentTransaction(final HttpContext httpCtx) {
        return ((AhcHttpContext) httpCtx).getHttpTransactionContext();
    }

    static HttpTransactionContext startTransaction(
            final Connection connection, final GrizzlyAsyncHttpProvider provider,
            final Request request, final GrizzlyResponseFuture future) {
        return new HttpTransactionContext(provider, connection, future, request);
    }

    // -------------------------------------------------------- Constructors

    private HttpTransactionContext(final GrizzlyAsyncHttpProvider provider,
                                   final Connection connection,
                                   final GrizzlyResponseFuture future, final Request ahcRequest) {

        this.provider = provider;
        this.connection = connection;
        this.future = future;
        this.ahcRequest = ahcRequest;
        redirectsAllowed = provider.getClientConfig().isFollowRedirect();
        maxRedirectCount = provider.getClientConfig().getMaxRedirects();
        this.requestUri = ahcRequest.getUri();
    }

    Connection getConnection() {
        return connection;
    }

    AsyncHandler getAsyncHandler() {
        return future.getAsyncHandler();
    }

    Request getAhcRequest() {
        return ahcRequest;
    }
    // ----------------------------------------------------- Private Methods

    HttpTransactionContext cloneAndStartTransactionFor(
            final Connection connection) {
        return cloneAndStartTransactionFor(connection, ahcRequest);
    }

    HttpTransactionContext cloneAndStartTransactionFor(
            final Connection connection,
            final Request request) {
        final HttpTransactionContext newContext = startTransaction(
                connection, provider, request, future);
        newContext.invocationStatus = invocationStatus;
        newContext.payloadGenerator = payloadGenerator;
        newContext.currentState = currentState;
        newContext.statusHandler = statusHandler;
        newContext.lastRedirectUri = lastRedirectUri;
        newContext.redirectCount = redirectCount;

        // detach the future
        future = null;

        return newContext;
    }

    boolean isGracefullyFinishResponseOnClose() {
        final HttpResponsePacket response = responsePacket;
        return response != null &&
               !response.getProcessingState().isKeepAlive() &&
               !response.isChunked() &&
               response.getContentLength() == -1;
    }

    void abort(final Throwable t) {
        if (future != null) {
            future.abort(t);
        }
    }

    void done() {
        done(null);
    }

    @SuppressWarnings(value = {"unchecked"})
    void done(Object result) {
        if (future != null) {
            future.done(result);
        }
    }

    boolean isTunnelEstablished(final Connection c) {
        return c.getAttributes().getAttribute("tunnel-established") != null;
    }

    void tunnelEstablished(final Connection c) {
        c.getAttributes().setAttribute("tunnel-established", Boolean.TRUE);
    }

    void reuseConnection() {
        this.isReuseConnection = true;
    }

    boolean isReuseConnection() {
        return isReuseConnection;
    }

    void touchConnection() {
        provider.touchConnection(connection, ahcRequest);
    }

    void closeConnection() {
        connection.closeSilently();
    }
} // END HttpTransactionContext
