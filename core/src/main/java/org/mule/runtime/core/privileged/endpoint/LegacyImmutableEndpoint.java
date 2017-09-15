/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.endpoint;

import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.core.api.MessageExchangePattern;

import java.io.Serializable;

/**
 * @deprecated Transport infrastructure is deprecated.
 */
@Deprecated
public interface LegacyImmutableEndpoint extends Serializable, NamedObject {

  MessageExchangePattern getExchangePattern();

}
