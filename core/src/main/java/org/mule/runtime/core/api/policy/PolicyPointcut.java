/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.policy;

/**
 * Provides a way to select which policies must be applied based on a given request.
 *
 * @since 4.0
 *
 * @deprecated Use {@link org.mule.runtime.policy.api.PolicyPointcut} directly.
 */
@Deprecated
public interface PolicyPointcut extends org.mule.runtime.policy.api.PolicyPointcut {

}
