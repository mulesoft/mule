/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.config.factory;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Operation;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.module.extension.mule.internal.config.provider.OperationDslBuildingDefinitionProvider.OperationBody;

import org.springframework.beans.factory.FactoryBean;

public class DefaultOperationFactoryBean extends AbstractComponent implements FactoryBean<Operation> {

  private String name;
  private ExtensionManager extensionManager;
  private OperationBody body;
  private MuleContext muleContext;

  @Override
  public Operation getObject() throws Exception {
    return null;
  }

  @Override
  public Class<?> getObjectType() {
    return Operation.class;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setExtensionManager(ExtensionManager extensionManager) {
    this.extensionManager = extensionManager;
  }

  public void setBody(OperationBody body) {
    this.body = body;
  }

  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }
}
