/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.description;

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static javax.lang.model.SourceVersion.RELEASE_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.module.extension.internal.resources.ExtensionResourcesGeneratorAnnotationProcessor.EXTENSION_VERSION;
import org.mule.runtime.api.meta.DescribedObject;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.AbstractAnnotationProcessorTestCase;
import org.mule.runtime.module.extension.internal.capability.xml.TestExtensionWithDocumentation;
import org.mule.runtime.module.extension.internal.loader.java.JavaModelLoaderDelegate;
import org.mule.tck.size.SmallTest;

import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.junit.Test;

@SmallTest
public class ExtensionDescriptionDeclarerTestCase extends AbstractAnnotationProcessorTestCase {

  @Test
  public void describeDescriptions() throws Exception {
    TestProcessor processor = new TestProcessor();
    assert_().about(javaSources()).that(testSourceFiles()).withCompilerOptions("-Aextension.version=1.0.0-dev")
        .processedWith(processor).compilesWithoutError();
    ExtensionDeclaration declaration = processor.getDeclaration();

    assertDescription(declaration, "Test Extension Description");
    List<ConfigurationDeclaration> configurations = declaration.getConfigurations();
    assertThat(configurations, hasSize(1));
    ConfigurationDeclaration onlyConfig = configurations.get(0);
    assertDescription(onlyConfig, "This is some documentation.");
    assertDescription(onlyConfig.getConnectionProviders().get(0), "Provider Documentation");

    List<ParameterDeclaration> params = onlyConfig.getAllParameters();
    assertDescription(params.get(0), "Config parameter");
    assertDescription(params.get(1), "Config Parameter with an Optional value");
    assertDescription(params.get(2), "Group parameter 1");
    assertDescription(params.get(3), "Group parameter 2");

    List<OperationDeclaration> operations = declaration.getOperations();
    OperationDeclaration operation = getOperationByName(operations, "operation");
    assertDescription(operation, "Test Operation");
    assertDescription(operation.getAllParameters().get(0), "test value");

    OperationDeclaration greetFriend = getOperationByName(operations, "greetFriend");
    assertDescription(greetFriend, "This method greets a friend");
    assertDescription(greetFriend.getAllParameters().get(0), "This is one of my friends");
    assertDescription(greetFriend.getAllParameters().get(1), "Some other friend");

    List<OperationDeclaration> connectedOperations = onlyConfig.getOperations();
    OperationDeclaration connectedOpe = connectedOperations.get(0);
    assertDescription(connectedOpe, "Test Operation with blank parameter description");
    assertDescription(connectedOpe.getAllParameters().get(0), "");
  }

  @SupportedAnnotationTypes(value = {"org.mule.runtime.extension.api.annotation.Extension"})
  @SupportedSourceVersion(RELEASE_8)
  @SupportedOptions(EXTENSION_VERSION)
  private class TestProcessor extends AbstractProcessor {

    private ExtensionDeclaration declaration;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      if (declaration == null) {
        ExtensionDescriptionDeclarer declarer = new ExtensionDescriptionDeclarer(processingEnv, roundEnv);
        Set<? extends Element> extensionElements = roundEnv.getElementsAnnotatedWith(Extension.class);
        assertThat(extensionElements, hasSize(1));
        Element extension = extensionElements.iterator().next();
        assertThat(extension, instanceOf(TypeElement.class));
        ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(Thread.currentThread().getContextClassLoader());
        JavaModelLoaderDelegate loader = new JavaModelLoaderDelegate(TestExtensionWithDocumentation.class, "1.0.0-dev");
        this.declaration = loader.declare(ctx).getDeclaration();
        declarer.document(declaration, (TypeElement) extension);
      }
      return false;
    }

    ExtensionDeclaration getDeclaration() {
      return declaration;
    }
  }

  private void assertDescription(DescribedObject object, String desc) {
    assertThat(object.getDescription(), is(desc));
  }

  private OperationDeclaration getOperationByName(List<OperationDeclaration> ops, String opeName) {
    return ops.stream().filter(operationModel -> operationModel.getName().equals(opeName)).findAny().orElse(null);
  }
}
