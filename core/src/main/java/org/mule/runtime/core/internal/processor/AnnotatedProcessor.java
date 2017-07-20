/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor;

import org.mule.runtime.api.meta.AnnotatedObject;
import org.mule.runtime.core.api.processor.Processor;

/**
 * A {@link Processor} that supports annotations.
 * 
 * @since 4.0
 */
public interface AnnotatedProcessor extends AnnotatedObject, Processor {

}
