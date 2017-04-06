/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.EXTENSION_DESCRIPTION;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.annotation.Extensible;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.ExtensionOf;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.property.ExtendingOperationModelProperty;

import org.junit.Test;

public class ExtensibleExtensionOperationsTestCase extends AbstractJavaExtensionDeclarationTestCase {

  private static final String SAY_HELLO_OPERATION = "sayHello";
  private static final String SAY_BYE_OPERATION = "sayBye";

  @Test
  public void operationIsExtensionOfSameExtension() throws Exception {
    setLoader(loaderFor(ExtensibleExtension.class));
    assertOperationExtensionOf(SAY_HELLO_OPERATION, ExtensibleExtension.class);
  }

  @Test
  public void operationIsExtensionOfDifferentExtension() {
    setLoader(loaderFor(ExtendingExtension.class));
    assertOperationExtensionOf(SAY_HELLO_OPERATION, ExtensibleExtension.class);
    assertOperationExtensionOf(SAY_BYE_OPERATION, ExtensibleExtension.class);
  }

  @Test
  public void operationIsExtensionOfSameAndDifferentExtension() {
    setLoader(loaderFor(ExtensibleExtendingExtension.class));
    assertOperationExtensionOf(SAY_HELLO_OPERATION, ExtensibleExtendingExtension.class);
    assertOperationExtensionOf(SAY_BYE_OPERATION, ExtensibleExtension.class);
  }

  private void assertOperationExtensionOf(String operationName, Class propertyType) {
    ExtensionDeclaration extensionDeclaration =
        getLoader().declare(new DefaultExtensionLoadingContext(getClass().getClassLoader(), getDefault(emptySet())))
            .getDeclaration();
    OperationDeclaration operation = getOperation(extensionDeclaration, operationName);

    ExtendingOperationModelProperty<ExtensibleExtension> modelProperty =
        operation.getModelProperty(ExtendingOperationModelProperty.class).get();
    assertThat(modelProperty.getType(), is(sameInstance(propertyType)));
  }

  @Extension(name = HEISENBERG, description = EXTENSION_DESCRIPTION)
  @Operations(ExtensibleExtensionOperation.class)
  @Extensible
  public static class ExtensibleExtension {

  }

  @Extension(name = HEISENBERG, description = EXTENSION_DESCRIPTION)
  @Operations({ClassLevelExtensionOfOperation.class, MethodLevelExtensionOfOperation.class})
  public static class ExtendingExtension {

  }

  @Extension(name = HEISENBERG, description = EXTENSION_DESCRIPTION)
  @Operations({MethodLevelExtensionOfOperation.class, ExtensibleExtensionOperation.class})
  @Extensible
  public static class ExtensibleExtendingExtension {

  }

  @ExtensionOf(ExtensibleExtension.class)
  private static class ClassLevelExtensionOfOperation {

    public String sayHello() {
      return "Hello!";
    }
  }


  private static class MethodLevelExtensionOfOperation {

    @ExtensionOf(ExtensibleExtension.class)
    public String sayBye() {
      return "Bye!";
    }
  }

  private static class ExtensibleExtensionOperation {

    public String sayHello() {
      return "Hello!";
    }
  }
}
