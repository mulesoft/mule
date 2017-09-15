/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
