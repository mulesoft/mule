/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.processor;

import org.mule.runtime.core.api.processor.Processor;

/**
 * A {@link Processor} that routes messages to zero or more destination message processors. Implementations determine exactly how
 * this is done by making decisions about which route(s) should be used and if the message should be copied or not.
 */
public interface Router extends Scope {

}
