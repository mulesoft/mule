/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.api.retry;

import org.mule.api.context.WorkManager;

import java.util.Map;


/**
 * A RetryPolicyTemplate creates a new {@link RetryPolicy} instance each time the retry goes into effect, 
 * thereby resetting any state the policy may have (counters, etc.)
 * 
 * A {@link RetryNotifier} may be set in order to take action upon each retry attempt.
 */
public interface RetryPolicyTemplate
{
    RetryPolicy createRetryInstance();

    Map<Object, Object> getMetaInfo();
    
    void setMetaInfo(Map<Object, Object> metaInfo);
    
    RetryNotifier getNotifier();

    void setNotifier(RetryNotifier retryNotifier);

    RetryContext execute(RetryCallback callback, WorkManager workManager) throws Exception;
}
