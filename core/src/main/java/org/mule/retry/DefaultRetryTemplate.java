/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.retry;

import org.mule.api.retry.PolicyFactory;
import org.mule.api.retry.RetryCallback;
import org.mule.api.retry.RetryContext;
import org.mule.api.retry.RetryNotifier;
import org.mule.api.retry.RetryTemplate;
import org.mule.api.retry.TemplatePolicy;
import org.mule.config.i18n.CoreMessages;
import org.mule.transport.FatalConnectException;

import java.io.InterruptedIOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A RetryTemplate can be used to invoke actions that may need to be retried i.e. connecting to an external process,
 * or dispatching an event. How retries are made is dictated by the {@link org.mule.api.retry.PolicyFactory}. Policies
 * are stategies that define what happens between retries.
 * Also a {@link org.mule.api.retry.RetryNotifier} that can be used to invoke actions between Retries for tracking and
 * notifications.
 *
 * @see org.mule.api.retry.RetryNotifier
 * @see RetryCallback
 * @see org.mule.api.retry.PolicyFactory
 */
public class DefaultRetryTemplate implements RetryTemplate
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(DefaultRetryTemplate.class);
    private final PolicyFactory policyFactory;
    private RetryNotifier notifier;

    public DefaultRetryTemplate(PolicyFactory policyFactory)
    {
        this(policyFactory, null);
    }

    public DefaultRetryTemplate(PolicyFactory policyFactory, RetryNotifier notifier)
    {
        if (policyFactory == null)
        {
            throw new IllegalArgumentException(CoreMessages.objectIsNull("policyFactory").getMessage());
        }
        this.policyFactory = policyFactory;
        this.notifier = notifier;
    }

    public RetryContext execute(RetryCallback callback) throws FatalConnectException
    {
        PolicyStatus status = null;
        TemplatePolicy policy = policyFactory.create();
        DefaultRetryContext context = new DefaultRetryContext(callback.getWorkDescription());
        try
        {
            do
            {
                try
                {
                    callback.doWork(context);
                    if (notifier != null)
                    {
                        notifier.sucess(context);
                    }

                    break;
                }
                catch (Exception e)
                {
                    if (notifier != null)
                    {
                        notifier.failed(context, e);
                    }
                    if (e instanceof InterruptedException || e instanceof InterruptedIOException)
                    {
                        logger.error("Process was interrupted (InterruptedException), ceasing process");
                        break;
                    }
                }
                status = policy.applyPolicy();
            }
            while (status.isOk());

            if(status==null || status.isOk())
            {
                return context;
            }
            else
            {
                throw new FatalConnectException(
                        CoreMessages.failedToConnect(context.getDescription(), policyFactory),
                        status.getThrowable(), this);
            }
        }
        finally
        {
            if (status != null && status.getThrowable() != null)
            {
                if (logger.isDebugEnabled())
                {
                    logger.error(status.getThrowable());
                }
            }
        }

    }

    public PolicyFactory getPolicyFactory()
    {
        return policyFactory;
    }

    public RetryNotifier getRetryNotifier()
    {
        return notifier;
    }
}
