/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.processor;

import org.mule.runtime.core.api.processor.Processor;

/**
 * A {@link Processor} that routes messages to zero or more destination message processors. Implementations determine exactly how
 * this is done by making decisions about which route(s) should be used and if the message should be copied or not.
 */
public interface Router extends Scope {

}
