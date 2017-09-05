/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.mvel.configuration;

/**
 * POJO for parsing the global-functions sub-element in the expression-language element.
 *
 * @since 4.0
 */
public class MVELGlobalFunctionsConfig {

  String inlineScript;
  String file;

  public String getInlineScript() {
    return inlineScript;
  }

  public void setInlineScript(String inlineScript) {
    this.inlineScript = inlineScript;
  }

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }
}
