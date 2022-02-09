/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.config.factory;

import static java.lang.String.format;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.findOperation;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Operation;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.dsl.api.component.ObjectFactory;
import org.mule.runtime.extension.api.exception.IllegalOperationModelDefinitionException;
import org.mule.runtime.module.extension.internal.runtime.operation.construct.MuleOperation;
import org.mule.runtime.module.extension.mule.internal.config.provider.OperationDslBuildingDefinitionProvider.OperationBody;

public class DefaultOperationObjectFactory extends AbstractComponent implements ObjectFactory<Operation> {

  private final String name;
  private final ExtensionManager extensionManager;
  private final MuleContext muleContext;
  private OperationBody body;

  public DefaultOperationObjectFactory(String name,
                                       ExtensionManager extensionManager,
                                       MuleContext muleContext) {
    this.name = name;
    this.extensionManager = extensionManager;
    this.muleContext = muleContext;
  }

  @Override
  public Operation getObject() {
    return MuleOperation.builder()
        .processors(body.getProcessors())
        .setOperationModel(locateOperationModel())
        .setChainLocation(body.getLocation())
        .setRootComponentLocation(body.getRootContainerLocation())
        .setMuleContext(muleContext)
        .build();
  }

  private OperationModel locateOperationModel() {
    ExtensionModel extensionModel = extensionManager.getExtension(muleContext.getConfiguration().getId())
        .orElseThrow(() -> new IllegalOperationModelDefinitionException(
            format("Cannot parse operation '%s'. Application ExtensionModel not found", name)));

    return findOperation(extensionModel, name)
        .orElseThrow(() -> new IllegalOperationModelDefinitionException(
            format("Cannot parse operation '%s'. OperationModel not found", name)));
  }

  public void setBody(OperationBody body) {
    this.body = body;
  }
}
