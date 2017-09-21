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
import static org.mule.runtime.extension.api.ExtensionConstants.EXPIRATION_POLICY_DESCRIPTION;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_DESCRIPTION;
import static org.mule.runtime.module.extension.internal.resources.ExtensionResourcesGeneratorAnnotationProcessor.EXTENSION_VERSION;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.compareXML;
import org.mule.runtime.api.meta.DescribedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
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
import org.mule.runtime.module.extension.internal.capability.xml.extension.multiple.config.TestExtensionWithDocumentationAndMultipleConfig;
import org.mule.runtime.module.extension.internal.capability.xml.extension.single.config.TestExtensionWithDocumentationAndSingleConfig;
import org.mule.runtime.module.extension.internal.loader.enricher.ExtensionDescriptionsEnricher;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.resources.documentation.ExtensionDocumentationResourceGenerator;
import org.mule.tck.size.SmallTest;

import com.google.testing.compile.JavaFileObjects;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@SmallTest
@RunWith(Parameterized.class)
public class ExtensionDescriptionDocumenterTestCase extends AbstractAnnotationProcessorTestCase {

  private static final String MULTIPLE_CONFIGS = "multipleConfigs";
  private static final String SINGLE_CONFIG = "singleConfig";

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {MULTIPLE_CONFIGS,
            TestExtensionWithDocumentationAndMultipleConfig.class,
            "/META-INF/multiple-extension-descriptions.xml",
            "src/test/java/org/mule/runtime/module/extension/internal/capability/xml/extension/multiple/config"},
        {SINGLE_CONFIG,
            TestExtensionWithDocumentationAndSingleConfig.class,
            "/META-INF/single-extension-descriptions.xml",
            "src/test/java/org/mule/runtime/module/extension/internal/capability/xml/extension/single/config"},
    });
  }

  private final String name;
  private final Class<?> extensionClass;
  private final String expectedProductPath;
  private final String sourcePath;

  public ExtensionDescriptionDocumenterTestCase(String name, Class<?> extension, String expectedProductPath, String sourcePath) {
    this.name = name;
    this.extensionClass = extension;
    this.expectedProductPath = expectedProductPath;
    this.sourcePath = sourcePath;
  }

  @Test
  public void persistDocumentation() throws Exception {
    InputStream in = getClass().getResourceAsStream(expectedProductPath);
    assertThat(in, is(notNullValue()));
    String expectedXml = IOUtils.toString(in);
    TestProcessor processor = new TestProcessor(extensionClass);
    doCompile(processor);
    ExtensionDocumentationResourceGenerator generator = new ExtensionDocumentationResourceGenerator();
    GeneratedResource resource = generator.generateResource(processor.getExtensionModel())
        .orElseThrow(() -> new RuntimeException("No Documentation Generated"));
    compareXML(expectedXml, new String(resource.getContent()));
  }

  @Test
  public void loadDocumentationFromFile() throws Exception {
    ClassLoader cl = currentThread().getContextClassLoader();
    ExtensionLoadingContext ctx = new DefaultExtensionLoadingContext(cl, getDefault(emptySet()));
    DefaultJavaModelLoaderDelegate loader = new DefaultJavaModelLoaderDelegate(extensionClass, "1.0.0-dev");
    loader.declare(ctx);
    ExtensionDescriptionsEnricher enricher = new ExtensionDescriptionsEnricher();
    enricher.enrich(ctx);
    ExtensionModelFactory factory = new ExtensionModelFactory();
    ExtensionModel extensionModel = factory.create(ctx);
    assertDescribedExtensionModel(extensionModel);
  }

  @Test
  public void describeDescriptions() throws Exception {
    TestProcessor processor = new TestProcessor(extensionClass);
    doCompile(processor);
    assertDescribedExtensionModel(processor.getExtensionModel());
  }

  private void assertDescribedExtensionModel(ExtensionModel extensionModel) {
    if (isSingleConfigTest()) {
      assertDescriptionsSingleConfig(extensionModel);
    } else {
      assertDescriptionsMultipleConfigs(extensionModel);
    }
  }

  private void assertDescriptionsMultipleConfigs(ExtensionModel declaration) {
    List<ConfigurationModel> configurations = declaration.getConfigurationModels();
    assertDescription(declaration, "Test Extension Description");
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
    assertDescription(params.get(2), EXPIRATION_POLICY_DESCRIPTION);
    assertDescription(params.get(3), "Group parameter 1");
    assertDescription(params.get(4), "Group parameter 2");

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

  private void assertDescriptionsSingleConfig(ExtensionModel extensionModel) {
    assertThat(extensionModel.getConfigurationModels(), hasSize(1));

    assertThat(extensionModel.getOperationModels(), hasSize(1));
    OperationModel withParameterGroup = extensionModel.getOperationModels().get(0);
    assertDescription(withParameterGroup, "Operation with parameter group");
    List<ParameterModel> operationParams = withParameterGroup.getAllParameterModels();
    assertDescription(operationParams.get(0), "value param description");
    assertDescription(operationParams.get(1), "First Description");
    assertDescription(operationParams.get(2), "Second Description");

    ConfigurationModel config = extensionModel.getConfigurationModels().get(0);
    assertDescription(extensionModel, "Test Extension Description with single config");

    assertDescription(config, DEFAULT_CONFIG_DESCRIPTION);
    assertDescription(config.getAllParameterModels().get(0), "Config parameter");
    assertDescription(config.getAllParameterModels().get(1), "Config Parameter with an Optional value");

    assertThat(config.getOperationModels(), hasSize(1));
    assertDescription(config.getOperationModels().get(0), "Operation with description");

    ConnectionProviderModel provider = config.getConnectionProviders().get(0);
    assertDescription(provider, "Provider description");
    assertDescription(provider.getAllParameterModels().get(0), "Connection Param Description");
    assertDescription(provider.getAllParameterModels().get(1), "First Description");
    assertDescription(provider.getAllParameterModels().get(2), "Second Description");
  }

  private void doCompile(TestProcessor processor) throws Exception {
    assert_().about(javaSources()).that(testSourceFiles()).withCompilerOptions("-Aextension.version=1.0.0-dev")
        .processedWith(processor).compilesWithoutError();
  }

  @SupportedAnnotationTypes(value = {"org.mule.runtime.extension.api.annotation.Extension"})
  @SupportedSourceVersion(RELEASE_8)
  @SupportedOptions(EXTENSION_VERSION)
  private class TestProcessor extends AbstractProcessor {

    private final Class<?> extensionClass;
    private ExtensionDeclaration declaration;
    private DefaultExtensionLoadingContext ctx;

    public TestProcessor(Class<?> extensionClass) {
      this.extensionClass = extensionClass;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
      if (declaration == null) {
        ExtensionDescriptionDocumenter documenter = new ExtensionDescriptionDocumenter(processingEnv, roundEnv);
        Element extension = roundEnv.getElementsAnnotatedWith(Extension.class).stream()
            .filter(element -> element.getSimpleName().contentEquals(extensionClass.getSimpleName()))
            .findFirst()
            .get();

        assertThat(extension, instanceOf(TypeElement.class));
        ctx = new DefaultExtensionLoadingContext(currentThread().getContextClassLoader(), getDefault(emptySet()));
        DefaultJavaModelLoaderDelegate loader = new DefaultJavaModelLoaderDelegate(extensionClass, "1.0.0-dev");
        declaration = loader.declare(ctx).getDeclaration();
        documenter.document(declaration, (TypeElement) extension);
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

  private boolean isSingleConfigTest() {
    return SINGLE_CONFIG.equals(name);
  }

  protected Iterable<JavaFileObject> testSourceFiles() throws Exception {
    // this will be xxx/target/test-classes
    File folder = new File(getClass().getClassLoader().getResource("").getPath().toString());
    // up to levels
    folder = folder.getParentFile().getParentFile();
    folder = new File(folder, sourcePath);
    File[] files = folder.listFiles((dir, name) -> name.endsWith(".java"));
    assertThat(files, is(notNullValue()));
    List<JavaFileObject> javaFileObjects = new ArrayList<>(files.length);
    for (File file : files) {
      javaFileObjects.add(JavaFileObjects.forResource(file.toURI().toURL()));
    }
    return javaFileObjects;
  }
}
