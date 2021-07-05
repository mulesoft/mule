/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.app.internal.declarer;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION_DEF;

import org.mule.runtime.api.component.TypedComponentIdentifier;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasOperationDeclarer;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

public class AppExtensionModelDeclarer {

  public void declare(ArtifactDescriptor artifactDescriptor, ArtifactType artifactType, ArtifactAst ast) {
    ExtensionDeclarer declarer = new ExtensionDeclarer();
    final String appName = artifactDescriptor.getName();
    declarer.named(appName)
            .describedAs("Extension model for " + artifactType.getAsString() + " " + appName)
            .onVersion(artifactDescriptor.getBundleDescriptor().getVersion());

    ast.topLevelComponentsStream()
            .filter(c -> c.getComponentType() == OPERATION_DEF)
            .forEach(c -> declareOperation(declarer, c));
  }

  private void declareOperation(HasOperationDeclarer declarer, ComponentAst ast) {
    ast.getParameter("").getValue().
    declarer.withOperation(ast.getParameter("").getResolvedRawValue())
  }
}
