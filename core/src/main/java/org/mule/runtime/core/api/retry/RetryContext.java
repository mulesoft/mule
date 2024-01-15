/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry;

import org.mule.api.annotation.NoImplement;

/**
 * The RetryContext is used to store any data which carries over from attempt to attempt such as response messages.
 */
@Deprecated
@NoImplement
public interface RetryContext extends org.mule.runtime.retry.api.RetryContext {

}
