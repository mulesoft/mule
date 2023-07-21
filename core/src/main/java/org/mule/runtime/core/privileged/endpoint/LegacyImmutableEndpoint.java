/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
