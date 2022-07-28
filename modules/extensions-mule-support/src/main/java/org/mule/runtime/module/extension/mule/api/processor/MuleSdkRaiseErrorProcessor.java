/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.api.processor;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.processor.AbstractRaiseErrorProcessor;

/**
 * Processor capable of raising errors within a Mule Operation's body on demand, given a type and optionally a message.
 *
 * @since 4.5
 */
public final class MuleSdkRaiseErrorProcessor extends AbstractRaiseErrorProcessor {

    public MuleSdkRaiseErrorProcessor() {
    }

    @Override
    protected ComponentIdentifier calculateErrorIdentifier(String typeId) {
        return builder().namespace("THIS").name(typeId).build();
    }

}
