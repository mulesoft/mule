/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security.oauth;

import org.mule.api.store.ObjectStore;

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestOAuth2Manager extends BaseOAuth2Manager<OAuth2Adapter>
{

    private final transient Logger logger = LoggerFactory.getLogger(TestOAuth2Manager.class);

    private KeyedPoolableObjectFactory<String, OAuth2Adapter> objectFactory;
    private OAuth2Adapter adapter;

    public TestOAuth2Manager(KeyedPoolableObjectFactory<String, OAuth2Adapter> objectFactory, OAuth2Adapter adapter)
    {
        this.objectFactory = objectFactory;
        this.adapter = adapter;
        this.setDefaultUnauthorizedConnector(this.adapter);
    }

    @Override
    protected Logger getLogger()
    {
        return logger;
    }

    @Override
    protected KeyedPoolableObjectFactory<String, OAuth2Adapter> createPoolFactory(OAuth2Manager<OAuth2Adapter> oauthManager,
                                                           ObjectStore<Serializable> objectStore)
    {
        return objectFactory;
    }

    @Override
    protected void fetchCallbackParameters(OAuth2Adapter adapter, String response)
    {
    }

    @Override
    protected void setCustomProperties(OAuth2Adapter adapter)
    {
    }

    @Override
    protected OAuth2Adapter instantiateAdapter()
    {
        return adapter;
    }
    
    @Override
    protected Set<Class<? extends Exception>> refreshAccessTokenOn()
    {
        Set<Class<? extends Exception>> exceptions = new HashSet<Class<? extends Exception>>();
        exceptions.add(FileNotFoundException.class);
        
        return exceptions;
    }
    
    @Override
    public KeyedPoolableObjectFactory<String, OAuth2Adapter> getAccessTokenPoolFactory()
    {
        return this.objectFactory;
    }
    

}
