/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.marvel.drstrange;

import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.stereotype.Stereotype;
import org.mule.runtime.extension.api.stereotype.MuleStereotypeDefinition;
import org.mule.runtime.extension.api.stereotype.ObjectStoreStereotype;

public class DrStrangeStereotypeDefinition extends MuleStereotypeDefinition {

  public static final String DR_STRANGE_STEREOTYPE_NAME = "DR_STRANGE_STEREOTYPE";

  @Override
  public String getName() {
    return DR_STRANGE_STEREOTYPE_NAME;
  }
}
