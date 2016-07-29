/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.extension.api.annotation.Exclusion;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

@Exclusion
public class ExclusiveCashier
{
    // @Optional not added intentionally to test the enforcement of optionality inside an @Exclusion class
    @Parameter
    String rothIRA;

    @Parameter
    @Optional
    String pensionPlan;
}
