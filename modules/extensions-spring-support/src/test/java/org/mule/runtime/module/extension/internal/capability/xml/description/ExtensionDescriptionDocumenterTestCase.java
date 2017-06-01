/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.description;

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;
import static java.lang.Thread.currentThread;
import static java.util.Collections.emptySet;
import static javax.lang.model.SourceVersion.RELEASE_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.module.extension.internal.resources.ExtensionResourcesGeneratorAnnotationProcessor.EXTENSION_VERSION;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.compareXML;
import org.mule.runtime.api.meta.DescribedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.ExtensionModelFactory;
import org.mule.runtime.module.extension.internal.AbstractAnnotationProcessorTestCase;
import org.mule.runtime.module.extension.internal.capability.xml.TestExtensionWithDocumentation;
import org.mule.runtime.module.extension.internal.loader.enricher.ExtensionDescriptionsEnricher;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.resources.documentation.ExtensionDocumentationResourceGenerator;
import org.mule.tck.size.SmallTest;

import java.io.InputStream;
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
public class ExtensionDescriptionDocumenterTestCase extends AbstractAnnotationProcessorTestCase {

  @Test
  public void persistDocumentation() throws Exception {
    InputStream in = getClass().getResourceAsStream("/META-INF/documentation-extension-descriptions.xml");
    assertThat(in, is(notNullValue()));
    String expectedXml = IOUtils.toString(in);
    TestProcessor processor = new TestProcessor();
    doCompile(processor);
    ExtensionDocumentationResourceGenerator generator = new ExtensionDocumentationResourceGenerator();
    GeneratedResource resource = generator.generateResource(processor.getExtensionModel())
        .orElseThrow(() -> new RuntimeException("No Documentation Generated"));
    compareXML(expectedXml, new String(resource.getContent()));
  }

  @Test
  public void loadDocumentationFromFile() throws Exception {
    ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(currentThread().getContextClassLoader(),
                                                                     getDefault(emptySet()));
    DefaultJavaModelLoaderDelegate loader = new DefaultJavaModelLoaderDelegate(TestExtensionWithDocumentation.class, "1.0.0-dev");
    loader.declare(ctx);
    ExtensionDescriptionsEnricher enricher = new ExtensionDescriptionsEnricher();
    enricher.enrich(ctx);
    ExtensionModelFactory factory = new ExtensionModelFactory();
    ExtensionModel extensionModel = factory.create(ctx);
    assertDescriptions(extensionModel);
  }

  @Test
  public void describeDescriptions() throws Exception {
    TestProcessor processor = new TestProcessor();
    doCompile(processor);
    assertDescriptions(processor.getExtensionModel());
  }

  private void assertDescriptions(ExtensionModel declaration) {
    assertDescription(declaration, "Test Extension Description");
    List<ConfigurationModel> configurations = declaration.getConfigurationModels();
    assertThat(configurations, hasSize(2));
    ConfigurationModel first = configurations.get(1);
    assertDescription(first, "This is some Config documentation.");
    assertDescription(first.getConnectionProviders().get(0), "Another Provider Documentation");
    assertDescription(first.getConnectionProviders().get(1), "Provider Documentation");

    ConfigurationModel second = configurations.get(0);
    assertDescription(second, "This is some Another Config documentation.");
    assertDescription(second.getConnectionProviders().get(0), "Another Provider Documentation");

    List<ParameterModel> params = first.getAllParameterModels();
    assertDescription(params.get(0), "Config parameter");
    assertDescription(params.get(1), "Config Parameter with an Optional value");
    assertDescription(params.get(2), "Group parameter 1");
    assertDescription(params.get(3), "Group parameter 2");

    List<OperationModel> operations = declaration.getOperationModels();
    OperationModel operation = getOperationByName(operations, "operation");
    assertDescription(operation, "Test Operation");
    assertDescription(operation.getAllParameterModels().get(0), "test value");

    OperationModel inheritedOperation = getOperationByName(operations, "inheritedOperation");
    assertDescription(inheritedOperation, "Inherited Operation Documentation");
    assertDescription(inheritedOperation.getAllParameterModels().get(0), "parameter documentation for an inherited operation.");

    OperationModel greetFriend = getOperationByName(operations, "greetFriend");
    assertDescription(greetFriend, "This method greets a friend");
    assertDescription(greetFriend.getAllParameterModels().get(0), "This is one of my friends");
    assertDescription(greetFriend.getAllParameterModels().get(1), "Some other friend");

    List<OperationModel> connectedOperations = first.getOperationModels();
    OperationModel connectedOpe = connectedOperations.get(0);
    assertDescription(connectedOpe, "Test Operation with blank parameter description");
    assertDescription(connectedOpe.getAllParameterModels().get(0), "");
  }

  private void doCompile(TestProcessor processor) throws Exception {
    assert_().about(javaSources()).that(testSourceFiles()).withCompilerOptions("-Aextension.version=1.0.0-dev")
        .processedWith(processor).compilesWithoutError();
  }

  @SupportedAnnotationTypes(value = {"org.mule.runtime.extension.api.annotation.Extension"})
  @SupportedSourceVersion(RELEASE_8)
  @SupportedOptions(EXTENSION_VERSION)
  private class TestProcessor extends AbstractProcessor {

    private ExtensionDeclaration declaration;
    private DefaultExtensionLoadingContext ctx;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      if (declaration == null) {
        ExtensionDescriptionDocumenter declarer = new ExtensionDescriptionDocumenter(processingEnv, roundEnv);
        Set<? extends Element> extensionElements = roundEnv.getElementsAnnotatedWith(Extension.class);
        assertThat(extensionElements, hasSize(1));
        Element extension = extensionElements.iterator().next();
        assertThat(extension, instanceOf(TypeElement.class));
        ctx = new DefaultExtensionLoadingContext(currentThread().getContextClassLoader(), getDefault(emptySet()));
        DefaultJavaModelLoaderDelegate loader = new DefaultJavaModelLoaderDelegate(TestExtensionWithDocumentation.class,
                                                                                   "1.0.0-dev");
        declaration = loader.declare(ctx).getDeclaration();
        declarer.document(declaration, (TypeElement) extension);
      }
      return false;
    }

    ExtensionModel getExtensionModel() {
      ExtensionModelFactory factory = new ExtensionModelFactory();
      return factory.create(ctx);
    }
  }

  private void assertDescription(DescribedObject object, String desc) {
    assertThat(object.getDescription(), is(desc));
  }

  private OperationModel getOperationByName(List<OperationModel> ops, String opeName) {
    return ops.stream().filter(operationModel -> operationModel.getName().equals(opeName)).findAny().orElse(null);
  }

}
