/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
