/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
