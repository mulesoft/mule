/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.execution;

import org.mule.runtime.core.internal.execution.MessageProcessPhase;

/**
 * Maker interface for every template that can be used in a {@link MessageProcessPhase}
 *
 * A {@link MessageProcessTemplate} must contain all the required method that redefines behavior inside a
 * {@link MessageProcessPhase} and it's particular from the {@link org.mule.runtime.core.api.source.MessageSource}
 *
 */
public interface MessageProcessTemplate {
}
