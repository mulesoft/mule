/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.application;

import org.mule.runtime.core.api.policy.PolicyProvider;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationPolicyManager;

/**
 * Provides and manages policies for an {@link Application}
 *
 * @since 4.0
 *
 */
public interface ApplicationPolicyProvider extends ApplicationPolicyManager, PolicyProvider {

}
