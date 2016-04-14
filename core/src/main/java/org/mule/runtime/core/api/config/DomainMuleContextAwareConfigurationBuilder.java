/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.config;

import org.mule.api.MuleContext;

/**
 * Configuration builders that implements this interface will receive a reference
 * to the context of the domain they belong to.
 */
public interface DomainMuleContextAwareConfigurationBuilder extends ConfigurationBuilder
{

    /**
     * @param domainContext MuleContext of the domain.
     */
    void setDomainContext(MuleContext domainContext);

}
