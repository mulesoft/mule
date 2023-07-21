/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.processor;

import org.mule.runtime.core.api.processor.Processor;

/**
 * A {@link Processor} that can contain one or multiple {@link Processor}s and applies logic common to those processors as
 * handling errors, managing transactions, etc.
 * 
 * @since 4.0
 */
public interface Scope extends Processor {

}
