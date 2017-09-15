/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import org.mule.runtime.api.exception.MuleException;

public interface Aggregator {

  void setTimeout(long timeout);

  void setFailOnTimeout(boolean failOnTimeout);

  void expireAggregation(String groupId) throws MuleException;

}
