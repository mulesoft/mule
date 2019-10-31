/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.module.http.api.HttpAuthentication;
import org.mule.module.http.internal.domain.request.HttpRequestAuthentication;
import org.mule.module.http.internal.domain.request.HttpRequestBuilder;
import org.mule.util.AttributeEvaluator;


public class DefaultHttpAuthentication implements HttpAuthentication, MuleContextAware, Initialisable
{
    private final HttpAuthenticationType type;

    private AttributeEvaluator username = new AttributeEvaluator(null);
    private AttributeEvaluator password = new AttributeEvaluator(null);
    private AttributeEvaluator domain = new AttributeEvaluator(null);
    private AttributeEvaluator workstation = new AttributeEvaluator(null);
    private AttributeEvaluator preemptive = new AttributeEvaluator(String.valueOf(false));

    private MuleContext muleContext;

    public DefaultHttpAuthentication(HttpAuthenticationType type)
    {
        this.type = type;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        username.initialize(muleContext.getExpressionManager());
        password.initialize(muleContext.getExpressionManager());
        domain.initialize(muleContext.getExpressionManager());
        workstation.initialize(muleContext.getExpressionManager());
        preemptive.initialize(muleContext.getExpressionManager());
    }

    public String getUsername()
    {
        return username.getRawValue();
    }

    public void setUsername(String username)
    {
        this.username = new AttributeEvaluator(username);
    }

    public String getPassword()
    {
        return password.getRawValue();
    }

    public void setPassword(String password)
    {
        this.password = new AttributeEvaluator(password);
    }

    public String getDomain()
    {
        return domain.getRawValue();
    }

    public void setDomain(String domain)
    {
        this.domain = new AttributeEvaluator(domain);
    }

    public HttpAuthenticationType getType()
    {
        return type;
    }

    public String getWorkstation()
    {
        return workstation.getRawValue();
    }

    public void setWorkstation(String workstation)
    {
        this.workstation = new AttributeEvaluator(workstation);
    }

    public String getPreemptive()
    {
        return preemptive.getRawValue();
    }

    public void setPreemptive(String preemptive)
    {
        this.preemptive = new AttributeEvaluator(preemptive);
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public HttpRequestAuthentication resolveRequestAuthentication(MuleEvent event)
    {
        HttpRequestAuthentication authentication = new HttpRequestAuthentication(type);
        authentication.setUsername(username.resolveStringValue(event));
        authentication.setPassword(password.resolveStringValue(event));
        authentication.setDomain(domain.resolveStringValue(event));
        authentication.setWorkstation(workstation.resolveStringValue(event));
        authentication.setPreemptive(preemptive.resolveBooleanValue(event));
        return authentication;
    }

    @Override
    public void authenticate(MuleEvent muleEvent, HttpRequestBuilder requestBuilder)
    {

    }

    @Override
    public boolean shouldRetry(MuleEvent firstAttemptResponseEvent)
    {
        return false;
    }


}
