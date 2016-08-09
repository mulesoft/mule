/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal;

import static org.apache.commons.collections.CollectionUtils.find;
import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.WithOperationsDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.spi.Describer;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.List;

public abstract class AbstractAnnotationsBasedDescriberTestCase extends AbstractMuleTestCase {

  private Describer describer;

  protected Describer getDescriber() {
    return describer;
  }

  protected void setDescriber(Describer describer) {
    this.describer = describer;
  }

  protected Describer describerFor(final Class<?> type) {
    return new AnnotationsBasedDescriber(type, new StaticVersionResolver(getProductVersion()));
  }

  protected ExtensionDeclarer describeExtension() {
    return getDescriber().describe(new DefaultDescribingContext(getClass().getClassLoader()));
  }

  protected ConfigurationDeclaration getConfiguration(ExtensionDeclaration extensionDeclaration, final String configurationName) {
    return (ConfigurationDeclaration) find(extensionDeclaration.getConfigurations(),
                                           object -> ((ConfigurationDeclaration) object).getName().equals(configurationName));
  }

  protected OperationDeclaration getOperation(WithOperationsDeclaration declaration, final String operationName) {
    return (OperationDeclaration) find(declaration.getOperations(),
                                       object -> ((OperationDeclaration) object).getName().equals(operationName));
  }

  protected ParameterDeclaration findParameter(List<ParameterDeclaration> parameters, final String name) {
    return (ParameterDeclaration) find(parameters, object -> name.equals(((ParameterDeclaration) object).getName()));
  }
}
