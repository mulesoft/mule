/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request;

import static org.apache.commons.lang.StringUtils.containsIgnoreCase;
import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.context.notification.BaseConnectorMessageNotification.MESSAGE_REQUEST_BEGIN;
import static org.mule.context.notification.BaseConnectorMessageNotification.MESSAGE_REQUEST_END;

import org.mule.DefaultMuleEvent;
import org.mule.OptimizedRequestContext;
import org.mule.RequestContext;
import org.mule.api.CompletionHandler;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.construct.FlowConstructAware;
import org.mule.api.context.MuleContextAware;
import org.mule.api.context.WorkManager;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleUtils;
import org.mule.api.MuleMessage;
import org.mule.config.i18n.CoreMessages;
import org.mule.construct.Flow;
import org.mule.context.notification.ConnectorMessageNotification;
import org.mule.context.notification.NotificationHelper;
import org.mule.module.http.api.HttpAuthentication;
import org.mule.module.http.internal.HttpParser;
import org.mule.module.http.internal.domain.request.HttpRequest;
import org.mule.module.http.internal.domain.request.HttpRequestAuthentication;
import org.mule.module.http.internal.domain.request.HttpRequestBuilder;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.processor.AbstractNonBlockingMessageProcessor;
import org.mule.util.AttributeEvaluator;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DefaultHttpRequester extends AbstractNonBlockingMessageProcessor implements Initialisable, MuleContextAware, FlowConstructAware
{

    public static final List<String> DEFAULT_EMPTY_BODY_METHODS = Lists.newArrayList("GET", "HEAD", "OPTIONS");
    public static final String DEFAULT_PAYLOAD_EXPRESSION = "#[payload]";
    public static final String DEFAULT_FOLLOW_REDIRECTS = "true";
    private static final Logger logger = LoggerFactory.getLogger(DefaultHttpRequester.class);

    private static final String REMOTELY_CLOSED = "Remotely closed";
    private static final int DEFAULT_RETRY_ATTEMPTS = 3;

    private DefaultHttpRequesterConfig requestConfig;
    private HttpRequesterRequestBuilder requestBuilder;
    private ResponseValidator responseValidator = new SuccessStatusCodeValidator("0..399");

    private AttributeEvaluator host = new AttributeEvaluator(null);
    private AttributeEvaluator port = new AttributeEvaluator(null);
    private AttributeEvaluator basePath = new AttributeEvaluator(null);
    private AttributeEvaluator path = new AttributeEvaluator(null);
    private AttributeEvaluator url = new AttributeEvaluator(null);

    private AttributeEvaluator method = new AttributeEvaluator("GET");
    private AttributeEvaluator followRedirects = new AttributeEvaluator(null);

    private AttributeEvaluator requestStreamingMode = new AttributeEvaluator(null);
    private AttributeEvaluator sendBodyMode = new AttributeEvaluator(null);
    private AttributeEvaluator parseResponse = new AttributeEvaluator(null);
    private AttributeEvaluator responseTimeout = new AttributeEvaluator(null);

    private String source;
    private String target;

    private MuleContext muleContext;
    private FlowConstruct flowConstruct;

    private MuleEventToHttpRequest muleEventToHttpRequest;
    private HttpResponseToMuleEvent httpResponseToMuleEvent;

    private NotificationHelper notificationHelper;

    @Override
    public void initialise() throws InitialisationException
    {
        if (requestConfig == null)
        {
            throw new InitialisationException(createStaticMessage("The config-ref attribute is required in the HTTP request element"), this);
        }
        if (requestBuilder == null)
        {
            requestBuilder = new HttpRequesterRequestBuilder();
        }
        LifecycleUtils.initialiseIfNeeded(requestBuilder);

        setEmptyAttributesFromConfig();
        validateRequiredProperties();

        basePath = new AttributeEvaluator(requestConfig.getBasePath());

        muleEventToHttpRequest = new MuleEventToHttpRequest(this, muleContext, requestStreamingMode, sendBodyMode);
        httpResponseToMuleEvent = new HttpResponseToMuleEvent(this, muleContext, parseResponse);

        initializeAttributeEvaluators(host, port, method, path, basePath, url, followRedirects,
                                      requestStreamingMode, sendBodyMode, parseResponse, responseTimeout);

        notificationHelper = new NotificationHelper(muleContext.getNotificationManager(), ConnectorMessageNotification.class, false);
    }

    private void setEmptyAttributesFromConfig() throws InitialisationException
    {
        if (host.getRawValue() == null)
        {
            setHost(requestConfig.getHost());
        }

        if (port.getRawValue() == null)
        {
            setPort(requestConfig.getPort());
        }

        if (followRedirects.getRawValue() == null)
        {
            String requestFollowRedirect = requestConfig.getFollowRedirects();
            if (requestFollowRedirect == null)
            {
                requestFollowRedirect = DEFAULT_FOLLOW_REDIRECTS;
            }
            setFollowRedirects(requestFollowRedirect);
        }

        if (requestStreamingMode.getRawValue() == null)
        {
            setRequestStreamingMode(requestConfig.getRequestStreamingMode());
        }

        if (sendBodyMode.getRawValue() == null)
        {
            setSendBodyMode(requestConfig.getSendBodyMode());
        }

        if (parseResponse.getRawValue() == null)
        {
            setParseResponse(requestConfig.getParseResponse());
        }

        if (responseTimeout.getRawValue() == null && requestConfig.getResponseTimeout() != null)
        {
            setResponseTimeout(requestConfig.getResponseTimeout());
        }
    }

    private void validateRequiredProperties() throws InitialisationException
    {
        if (url.getRawValue() == null)
        {
            if (host.getRawValue() == null)
            {
                throw new InitialisationException(createStaticMessage("No host defined. Set the host attribute " +
                                                                      "either in the request or request-config elements"), this);
            }
            if (port.getRawValue() == null)
            {
                throw new InitialisationException(createStaticMessage("No port defined. Set the host attribute " +
                                                                      "either in the request or request-config elements"), this);
            }
            if (path.getRawValue() == null)
            {
                throw new InitialisationException(createStaticMessage("The path attribute is required in the HTTP request element"), this);
            }
        }
    }

    private void initializeAttributeEvaluators(AttributeEvaluator... attributeEvaluators)
    {
        for (AttributeEvaluator attributeEvaluator : attributeEvaluators)
        {
            if (attributeEvaluator != null)
            {
                attributeEvaluator.initialize(muleContext.getExpressionManager());
            }
        }
    }

    @Override
    protected MuleEvent processBlocking(final MuleEvent muleEvent) throws MuleException
    {
        return innerProcess(muleEvent, DEFAULT_RETRY_ATTEMPTS);
    }

    @Override
    protected void processNonBlocking(final MuleEvent muleEvent, final CompletionHandler completionHandler) throws
                                                                                                            MuleException
    {
        innerProcessNonBlocking(muleEvent, completionHandler, DEFAULT_RETRY_ATTEMPTS);
    }

    protected void innerProcessNonBlocking(final MuleEvent muleEvent, final CompletionHandler originalCompletionHandler,
                                           final int retryCount) throws MuleException
    {
        final HttpAuthentication authentication = requestConfig.getAuthentication();
        final HttpRequest httpRequest = createHttpRequest(muleEvent, authentication);

        notificationHelper.fireNotification(muleEvent, httpRequest.getUri(), muleEvent.getFlowConstruct(), MESSAGE_REQUEST_BEGIN);

        getHttpClient().send(httpRequest, resolveResponseTimeout(muleEvent),
                             followRedirects.resolveBooleanValue(muleEvent),
                             resolveAuthentication(muleEvent),
                             createNonBlockingCompletionHandler(muleEvent, originalCompletionHandler, retryCount, authentication, httpRequest),
                             getWorkManager(muleEvent));
    }

    private CompletionHandler<HttpResponse, Exception> createNonBlockingCompletionHandler(final MuleEvent muleEvent,
                                                                                          final CompletionHandler originalCompletionHandler,
                                                                                          final int retryCount,
                                                                                          final HttpAuthentication authentication,
                                                                                          final HttpRequest httpRequest)
    {
        return new CompletionHandler<HttpResponse, Exception>()
        {
            @Override
            public void onFailure(Exception exception)
            {
                try
                {
                    MessagingException msgException = new MessagingException(createStaticMessage("Error sending HTTP request"),
                                                                             resetMuleEventForNewThread(muleEvent),
                                                                             exception,
                                                                             DefaultHttpRequester.this);
                    // Only retry request in case of race condition where connection is closed after it is obtained from pool causing
                    // a "IOException: Remotely Closed"
                    if (shouldRetryRemotelyClosed(exception, retryCount))
                    {
                        try
                        {
                            innerProcessNonBlocking(muleEvent, originalCompletionHandler, retryCount - 1);
                        }
                        catch (MuleException e)
                        {
                            // Only exception caught here is from createHttpRequest, so it doesn't make sense to use error message
                            // from http request but instead simply propagate exception as happens the first time around.
                            originalCompletionHandler.onFailure(new MessagingException(muleEvent, e, DefaultHttpRequester.this));
                        }
                    }
                    else
                    {
                        originalCompletionHandler.onFailure(msgException);
                    }
                }
                finally
                {
                    RequestContext.clear();
                }
            }

            @Override
            public void onCompletion(HttpResponse httpResponse)
            {
                try
                {

                    MuleMessage originalMuleMessage = muleEvent.getMessage();
                    httpResponseToMuleEvent.convert(muleEvent, httpResponse);
                    notificationHelper.fireNotification(muleEvent, httpRequest.getUri(),
                                                        muleEvent.getFlowConstruct(), MESSAGE_REQUEST_END);
                    resetMuleEventForNewThread(muleEvent);


                    if (resendRequest(muleEvent, retryCount, authentication))
                    {
                        consumePayload(muleEvent);
                        muleEvent.setMessage(originalMuleMessage);
                        innerProcessNonBlocking(muleEvent, originalCompletionHandler, 0);
                    }
                    else
                    {
                        validateResponse(muleEvent);
                        originalCompletionHandler.onCompletion(muleEvent);
                    }
                }
                catch (MessagingException messagingException)
                {
                    originalCompletionHandler.onFailure(messagingException);
                }
                catch (MuleException muleException)
                {
                    originalCompletionHandler.onFailure(new MessagingException(resetMuleEventForNewThread(muleEvent), muleException, DefaultHttpRequester.this));
                }
                finally
                {
                    RequestContext.clear();
                }
            }

            private MuleEvent resetMuleEventForNewThread(MuleEvent event)
            {
                // Reset access control for new thread
                ((DefaultMuleEvent) event).resetAccessControl();
                // Set RequestContext ThreadLocal in new thread for backwards compatibility
                OptimizedRequestContext.unsafeSetEvent(event);
                return event;
            }
        };
    }

    private boolean shouldRetryRemotelyClosed(Exception exception, int retryCount)
    {
        boolean shouldRetry = exception instanceof IOException && containsIgnoreCase(exception.getMessage(), REMOTELY_CLOSED) && retryCount > 0;
        if (shouldRetry)
        {
            logger.warn("Sending HTTP message failed with `" + IOException.class.getCanonicalName() + ": " + REMOTELY_CLOSED
                        + "`. Request will be retried " + retryCount + " time(s) before failing.");
        }
        return shouldRetry;
    }

    private WorkManager getWorkManager(MuleEvent event)
    {
        FlowConstruct currentFlowConstruct = flowConstruct != null ? flowConstruct : event.getFlowConstruct();
        if (currentFlowConstruct != null && currentFlowConstruct instanceof Flow)
        {
            return ((Flow) currentFlowConstruct).getWorkManager();
        }
        else
        {
            return null;
        }
    }

    private MuleEvent innerProcess(MuleEvent muleEvent, int retryCount) throws MuleException
    {
        HttpAuthentication authentication = requestConfig.getAuthentication();
        HttpRequest httpRequest = createHttpRequest(muleEvent, authentication);
        HttpResponse response;
        try
        {
            notificationHelper.fireNotification(muleEvent, httpRequest.getUri(), muleEvent.getFlowConstruct(), MESSAGE_REQUEST_BEGIN);
            response = getHttpClient().send(httpRequest, resolveResponseTimeout(muleEvent), followRedirects.resolveBooleanValue(muleEvent), resolveAuthentication(muleEvent));
        }
        catch (Exception e)
        {
            // Only retry request in case of race condition where connection is closed after it is obtained from pool causing
            // a "IOException: Remotely Closed"
            if (shouldRetryRemotelyClosed(e, retryCount))
            {
                return innerProcess(muleEvent, retryCount - 1);
            }
            else
            {
                throw new MessagingException(CoreMessages.createStaticMessage("Error sending HTTP request"), muleEvent, e, this);
            }
        }

        MuleMessage originalMuleMessage = muleEvent.getMessage();
        httpResponseToMuleEvent.convert(muleEvent, response);
        notificationHelper.fireNotification(muleEvent, httpRequest.getUri(), muleEvent.getFlowConstruct(), MESSAGE_REQUEST_END);

        if (resendRequest(muleEvent, retryCount, authentication))
        {
            consumePayload(muleEvent);
            muleEvent.setMessage(originalMuleMessage);
            muleEvent = innerProcess(muleEvent, 0);
        }
        else
        {
            validateResponse(muleEvent);
        }
        return muleEvent;
    }

    private HttpClient getHttpClient()
    {
        return requestConfig.getHttpClient();
    }

    private void validateResponse(MuleEvent muleEvent) throws ResponseValidatorException
    {
        responseValidator.validate(muleEvent);
    }

    private boolean resendRequest(MuleEvent muleEvent, int retryCount, HttpAuthentication authentication) throws MuleException
    {
        return retryCount > 0 && authentication != null && authentication.shouldRetry(muleEvent);
    }

    private HttpRequest createHttpRequest(MuleEvent muleEvent, HttpAuthentication authentication) throws MuleException
    {
        HttpRequestBuilder builder = muleEventToHttpRequest.create(muleEvent, method.resolveStringValue(muleEvent), resolveURI(muleEvent));

        if (authentication != null)
        {
            authentication.authenticate(muleEvent, builder);
        }
        return builder.build();
    }

    private HttpRequestAuthentication resolveAuthentication(MuleEvent event)
    {
        HttpRequestAuthentication requestAuthentication = null;

        if (requestConfig.getAuthentication() instanceof DefaultHttpAuthentication)
        {
            requestAuthentication = ((DefaultHttpAuthentication) requestConfig.getAuthentication()).resolveRequestAuthentication(event);
        }
        return requestAuthentication;
    }

    private int resolveResponseTimeout(MuleEvent muleEvent)
    {
        if (responseTimeout.getRawValue() == null)
        {
            return muleEvent.getTimeout();
        }
        else
        {
            return responseTimeout.resolveIntegerValue(muleEvent);
        }
    }

    private String resolveURI(MuleEvent muleEvent) throws MessagingException
    {
        if (url.getRawValue() != null)
        {
            return url.resolveStringValue(muleEvent);
        }
        else
        {
            String resolvedPath = replaceUriParams(buildPath(basePath.resolveStringValue(muleEvent),
                                                             path.resolveStringValue(muleEvent)), muleEvent);

            // Encode spaces to generate a valid HTTP request.
            resolvedPath = HttpParser.encodeSpaces(resolvedPath);

            return String.format("%s://%s:%s%s", requestConfig.getScheme(), host.resolveStringValue(muleEvent),
                                 port.resolveIntegerValue(muleEvent), resolvedPath);
        }

    }


    private String replaceUriParams(String path, MuleEvent event)
    {
        if (requestBuilder == null)
        {
            return path;
        }
        else
        {
            return requestBuilder.replaceUriParams(path, event);
        }
    }

    protected String buildPath(String basePath, String path)
    {
        String resolvedBasePath = basePath;
        String resolvedRequestPath = path;

        if (!resolvedBasePath.startsWith("/"))
        {
            resolvedBasePath = "/" + resolvedBasePath;
        }

        if (resolvedBasePath.endsWith("/") && resolvedRequestPath.startsWith("/"))
        {
            resolvedBasePath = resolvedBasePath.substring(0, resolvedBasePath.length() - 1);
        }

        if (!resolvedBasePath.endsWith("/") && !resolvedRequestPath.startsWith("/") && !resolvedRequestPath.isEmpty())
        {
            resolvedBasePath += "/";
        }


        return resolvedBasePath + resolvedRequestPath;

    }

    private void consumePayload(final MuleEvent event)
    {
        if (event.getMessage().getPayload() instanceof InputStream)
        {
            try
            {
                event.getMessage().getPayloadAsBytes();
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(e);
            }
        }
    }

    public String getHost()
    {
        return host.getRawValue();
    }

    public void setHost(String host)
    {
        this.host = new AttributeEvaluator(host);
    }

    public String getPort()
    {
        return port.getRawValue();
    }

    public void setPort(String port)
    {
        this.port = new AttributeEvaluator(port);
    }

    public String getPath()
    {
        return path.getRawValue();
    }

    public void setPath(String path)
    {
        this.path = new AttributeEvaluator(path);
    }

    public String getUrl()
    {
        return url.getRawValue();
    }

    public void setUrl(String url)
    {
        this.url = new AttributeEvaluator(url);
    }

    public HttpRequesterRequestBuilder getRequestBuilder()
    {
        return requestBuilder;
    }

    public void setRequestBuilder(HttpRequesterRequestBuilder requestBuilder)
    {
        this.requestBuilder = requestBuilder;
    }

    public String getMethod()
    {
        return method.getRawValue();
    }

    public void setMethod(String method)
    {
        this.method = new AttributeEvaluator(method);
    }

    public DefaultHttpRequesterConfig getConfig()
    {
        return requestConfig;
    }

    public void setConfig(DefaultHttpRequesterConfig requestConfig)
    {
        this.requestConfig = requestConfig;
    }

    public void setFollowRedirects(String followsRedirects)
    {
        this.followRedirects = new AttributeEvaluator(followsRedirects);
    }

    public void setRequestStreamingMode(String requestStreamingMode)
    {
        this.requestStreamingMode = new AttributeEvaluator(requestStreamingMode);
    }

    public ResponseValidator getResponseValidator()
    {
        return responseValidator;
    }

    public void setResponseValidator(ResponseValidator responseValidator)
    {
        this.responseValidator = responseValidator;
    }

    public void setSendBodyMode(String sendBodyMode)
    {
        this.sendBodyMode = new AttributeEvaluator(sendBodyMode);
    }

    public String getSource()
    {
        return source;
    }

    public void setSource(String source)
    {
        this.source = source;
    }

    public String getTarget()
    {
        return target;
    }

    public void setTarget(String target)
    {
        this.target = target;
    }

    public void setParseResponse(String parseResponse)
    {
        this.parseResponse = new AttributeEvaluator(parseResponse);
    }

    public void setResponseTimeout(String responseTimeout)
    {
        this.responseTimeout = new AttributeEvaluator(responseTimeout);
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    @Override
    public void setFlowConstruct(FlowConstruct flowConstruct)
    {
        this.flowConstruct = flowConstruct;
    }
}
