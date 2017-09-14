/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.processor.Processor;

/**
 * Abstract class for {@link Processor}s that must also be annotated object.
 *
 * This is usually the case of processors created from the configuration elements.
 */
public abstract class AbstractProcessor extends AbstractComponent implements Processor {

}
