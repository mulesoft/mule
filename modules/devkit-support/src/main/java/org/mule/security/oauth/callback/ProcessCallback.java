/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth.callback;

import org.mule.api.devkit.ProcessTemplate;

import java.util.List;

/**
 * Callback with logic to execute within a controlled environment provided by {@link ProcessTemplate}
 *  @param <T> type of the return value of the processing execution
 * 
 */
public abstract class ProcessCallback<T, O>
{

    private String accessTokenId;
    
    public abstract T process(O object) throws Exception;

    public abstract List<Class<? extends Exception>> getManagedExceptions();

    public abstract boolean isProtected();

    public String getAccessTokenId()
    {
        return accessTokenId;
    }

    public void setAccessTokenId(String accessTokenId)
    {
        this.accessTokenId = accessTokenId;
    }
    
}
