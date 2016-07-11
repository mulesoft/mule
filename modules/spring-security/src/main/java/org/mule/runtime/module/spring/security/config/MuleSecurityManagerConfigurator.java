/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.spring.security.config;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import org.mule.runtime.config.spring.dsl.api.ObjectFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.security.SecurityProvider;
import org.mule.runtime.core.security.MuleSecurityManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * {@code ObjectFactory} for a security manager configuration element.
 * <p>
 * In case the security manager has no name or the name is the same as the default one
 * provided by the {@code MuleContext} then the configured values will be added only to the
 * security manager, otherwise a new security manager will be registered under the provided name
 * but the addition of providers will be done to both, the new security manager and the default one.
 *
 * @since 4.0
 */
public class MuleSecurityManagerConfigurator implements ObjectFactory<SecurityManager>
{

    private List<SecurityProvider> providers = new ArrayList<>();
    private MuleContext muleContext;
    private String name = OBJECT_SECURITY_MANAGER;

    public void setName(String name)
    {
        this.name = name;
    }

    @Inject
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public void setProviders(List<SecurityProvider> providers)
    {
        this.providers = providers;
    }

    @Override
    public SecurityManager getObject() throws Exception
    {
        List<SecurityManager> securityManagers = new ArrayList<>();
        securityManagers.add(muleContext.getSecurityManager());
        SecurityManager factorySecurityManager = muleContext.getSecurityManager();
        if (!name.equals(OBJECT_SECURITY_MANAGER))
        {
            factorySecurityManager = new MuleSecurityManager();
            securityManagers.add(factorySecurityManager);
        }
        providers.stream().forEach(provider -> {
            securityManagers.stream().forEach(securityManager -> {
                securityManager.addProvider(provider);
            });
        });
        return factorySecurityManager;
    }
}
