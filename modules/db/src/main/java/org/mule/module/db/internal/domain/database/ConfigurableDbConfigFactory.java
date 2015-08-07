/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.domain.database;

import org.mule.api.retry.RetryPolicyTemplate;
import org.mule.module.db.internal.domain.type.DbType;

import java.util.List;

/**
 * Provides a way to configure a {@link DbConfigFactory}
 */
public interface ConfigurableDbConfigFactory extends DbConfigFactory
{

    /**
     * Sets the list of custom types available for the created {@link DbConfig}
     * @param customDataTypes list of custom types. Non null
     */
    void setCustomDataTypes(List<DbType> customDataTypes);

    /**
     * Sets the retry policy template for the created {@link DbConfig}
     *
     * @param retryPolicyTemplate retry policety template. Can be null.
     */
    void setRetryPolicyTemplate(RetryPolicyTemplate retryPolicyTemplate);
}
