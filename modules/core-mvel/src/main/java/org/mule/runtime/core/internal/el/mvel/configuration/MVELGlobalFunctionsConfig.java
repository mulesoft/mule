/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.el.mvel.configuration;

import org.mule.runtime.api.component.AbstractComponent;

/**
 * POJO for parsing the global-functions sub-element in the expression-language element.
 *
 * @since 4.0
 */
public class MVELGlobalFunctionsConfig extends AbstractComponent {

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
