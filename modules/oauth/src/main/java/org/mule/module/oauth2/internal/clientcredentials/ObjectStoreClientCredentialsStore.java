/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.oauth2.internal.clientcredentials;

import org.mule.api.store.ListableObjectStore;
import org.mule.util.store.ObjectStoreToMapAdapter;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class ObjectStoreClientCredentialsStore implements ClientCredentialsStore
{

    private static final String ACCESS_TOKEN_KEY = "_accessToken";
    private static final String EXPIRES_IN_KEY = "_expiresIn";

    private final Map<String, Object> objectStore;

    public ObjectStoreClientCredentialsStore(ListableObjectStore objectStore)
    {
        this.objectStore = new ObjectStoreToMapAdapter(objectStore);
    }

    public void storeAccessToken(String accessToken)
    {
        objectStore.put(ACCESS_TOKEN_KEY, accessToken);
    }

    public void storeExpiresIn(String expiresIn)
    {
        objectStore.put(EXPIRES_IN_KEY, expiresIn);
    }

    public void storeCustomParameter(final String parameterName, final Object parameterValue)
    {
        objectStore.put(parameterName, parameterValue);
    }

    public String getAccessToken()
    {
        return (String) objectStore.get(ACCESS_TOKEN_KEY);
    }

    public String getExpiresIn()
    {
        return (String) objectStore.get(EXPIRES_IN_KEY);
    }

    @Override
    public Collection<String> getTokenResponseParametersNames()
    {
        final Set<String> allKeys = objectStore.keySet();
        allKeys.remove(ACCESS_TOKEN_KEY);
        allKeys.remove(EXPIRES_IN_KEY);
        return allKeys;
    }

    @Override
    public Object getTokenResponseParameters(String parameterName)
    {
        return objectStore.get(parameterName);
    }

}
