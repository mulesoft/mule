/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.processor;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.processor.Processor;

/**
 * A {@link Processor} that supports annotations.
 * 
 * @since 4.0
 */
@NoImplement
public interface AnnotatedProcessor extends Component, Processor {

}
