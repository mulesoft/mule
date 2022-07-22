/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.forward.internal;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.sdk.api.utils.ForwardCompatibilityHelper;

public class ForwardCompatibilityOperations {

    @MediaType(value = TEXT_PLAIN)
    public String getHelperClassName(){
        return ForwardCompatibilityHelper.getInstance().getClass().getName();
    }
}
