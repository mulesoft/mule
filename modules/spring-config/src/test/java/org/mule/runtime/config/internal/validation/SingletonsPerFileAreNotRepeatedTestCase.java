/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.validation;

import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@SmallTest
@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
public class SingletonsPerFileAreNotRepeatedTestCase extends AbstractMuleTestCase {

  private final SingletonsPerFileAreNotRepeated validation = new SingletonsPerFileAreNotRepeated();
}
