/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.model.extension.xml;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.core.privileged.execution.LocationExecutionContextProvider;

/**
 * Internal object responsible to traverse the entire XMl of a given application to read the {@link ComponentModel} to
 * recreate the macro expanded XML of the application.
 *
 * It will obfuscate attributes that matches with {@link LocationExecutionContextProvider#maskPasswords(String, String)}.
 * @since 4.2.0
 */
class ComponentModelReaderHelper {

  static final String PASSWORD_MASK = "@@credentials@@";

  private ComponentModelReaderHelper() {}

  static String toXml(ComponentAst rootComponentModel) {
    return rootComponentModel.getMetadata().getSourceCode()
        .orElse("");
  }

}
