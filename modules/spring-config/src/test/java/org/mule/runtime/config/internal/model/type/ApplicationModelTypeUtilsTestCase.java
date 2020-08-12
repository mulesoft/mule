/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.model.type;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Optional.ofNullable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mule.runtime.config.internal.model.type.ApplicationModelTypeUtils.resolveTypedComponentIdentifier;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ARTIFACT_AST;
import static org.mule.test.allure.AllureConstants.ArtifactAst.ParameterAst.PARAMETER_AST;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.internal.dsl.model.ExtensionModelHelper;
import org.mule.runtime.config.internal.dsl.model.ExtensionModelHelper.ExtensionWalkerModelDelegate;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.List;
import java.util.Map;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ARTIFACT_AST)
@Story(PARAMETER_AST)
public class ApplicationModelTypeUtilsTestCase extends AbstractMuleTestCase {

  private static final XmlDslModel XML_DSL_MODEL = XmlDslModel.builder()
      .setPrefix("mockns")
      .setNamespace("http://mockns")
      .build();

  private static final ComponentIdentifier SIMPLE_POJO_ID = ComponentIdentifier.builder()
      .namespaceUri(XML_DSL_MODEL.getNamespace())
      .namespace(XML_DSL_MODEL.getPrefix())
      .name("simple-pojo")
      .build();

  private static final ComponentIdentifier COMPLEX_POJO_ID = ComponentIdentifier.builder()
      .namespaceUri(XML_DSL_MODEL.getNamespace())
      .namespace(XML_DSL_MODEL.getPrefix())
      .name("complex-pojo")
      .build();

  private static final ComponentIdentifier SIMPLE_ID = ComponentIdentifier.builder()
      .namespaceUri(XML_DSL_MODEL.getNamespace())
      .namespace(XML_DSL_MODEL.getPrefix())
      .name("simple")
      .build();

  private ExtensionModel extensionModel;
  private ExtensionModelHelper extModelHelper;

  @Before
  public void before() {
    final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault()
        .createTypeLoader(ApplicationModelTypeUtilsTestCase.class.getClassLoader());

    extensionModel = mock(ExtensionModel.class);
    when(extensionModel.getXmlDslModel()).thenReturn(XML_DSL_MODEL);

    final DslSyntaxResolver dslSyntaxResolver = DslSyntaxResolver.getDefault(extensionModel, mock(DslResolvingContext.class));

    extModelHelper = mock(ExtensionModelHelper.class);

    when(extModelHelper.findMetadataType(any(Class.class)))
        .thenAnswer(inv -> ofNullable(typeLoader.load(inv.getArgument(0, Class.class))));
    when(extModelHelper.findParameterModel(any(), any()))
        .thenAnswer(inv -> {
          ComponentIdentifier nestedComponentId = inv.getArgument(0);
          ParameterizedModel model = inv.getArgument(1);

          return model.getAllParameterModels()
              .stream()
              .filter(pm -> dslSyntaxResolver.resolve(pm).getElementName().equals(nestedComponentId.getName()))
              .findAny();
        });

    doAnswer(inv -> {
      final ComponentIdentifier identifier = inv.getArgument(0);
      final ExtensionWalkerModelDelegate walker = inv.getArgument(2);

      if (identifier.equals(SIMPLE_POJO_ID)) {
        walker.onType(typeLoader.load(SimplePojo.class));
      } else if (identifier.equals(COMPLEX_POJO_ID)) {
        walker.onType(typeLoader.load(ComplexPojo.class));
      } else if (identifier.equals(SIMPLE_ID)) {
        walker.onType(typeLoader.load(SimplePojo.class));
      }

      return null;
    })
        .when(extModelHelper).walkToComponent(any(), eq(false), any());

    doAnswer(inv -> {
      final ParameterizedModel parameterized = inv.getArgument(0);
      final ComponentIdentifier identifier = inv.getArgument(1);

      return dslSyntaxResolver.resolve(parameterized);
    })
        .when(extModelHelper).resolveDslElementModel(any(NamedObject.class), any());

    doAnswer(inv -> {
      final ParameterModel parameterized = inv.getArgument(0);
      final ComponentIdentifier identifier = inv.getArgument(1);

      return dslSyntaxResolver.resolve(parameterized);
    })
        .when(extModelHelper).resolveDslElementModel(any(ParameterModel.class), any());
  }

  @Test
  public void simpleParamInSimplePojo() {
    final ComponentModel component = createModel(SIMPLE_POJO_ID,
                                                 singletonMap("pojoSimpleParam", "value"));

    resolveTypedComponentIdentifier(component, false, extModelHelper);

    verify(component).setParameter(argThat(new ParameterModelMatcher("pojoSimpleParam")),
                                   argThat(new ParameterRawValueModelMatcher("value")));
    verify(component).setMetadataTypeModelAdapter(any());
  }

  @Test
  public void pojoParamInComplexPojo() {
    final ComponentModel innerComponent = createModel(SIMPLE_ID,
                                                      singletonMap("pojoSimpleParam", "value"));
    final ComponentModel component = createModel(COMPLEX_POJO_ID,
                                                 emptyMap(),
                                                 asList(innerComponent));

    resolveTypedComponentIdentifier(component, false, extModelHelper);

    verify(component).setParameter(argThat(new ParameterModelMatcher("simple")),
                                   argThat(new ParameterNestedValueModelMatcher(innerComponent)));
    verify(component).setMetadataTypeModelAdapter(any());

    verify(innerComponent).setParameter(argThat(new ParameterModelMatcher("pojoSimpleParam")),
                                        argThat(new ParameterRawValueModelMatcher("value")));
    verify(innerComponent).setMetadataTypeModelAdapter(any());
  }

  private ComponentModel createModel(ComponentIdentifier identifier, Map<String, String> rawParams,
                                     List<ComponentModel> children) {
    final ComponentModel component = createModel(identifier, rawParams);
    when(component.directChildrenStream()).thenReturn(children.stream().map(c -> (ComponentModel) c));
    when(component.getInnerComponents()).thenReturn(children);

    return component;
  }

  private ComponentModel createModel(ComponentIdentifier identifier, Map<String, String> rawParams) {
    final ComponentModel component = mock(ComponentModel.class);

    when(component.getIdentifier()).thenReturn(identifier);
    when(component.getRawParameters()).thenReturn(rawParams);
    when(component.getRawParameterValue(anyString())).thenAnswer(inv -> ofNullable(rawParams.get(inv.getArgument(0))));

    return component;
  }

  public static class SimplePojo {

    @Parameter
    private String pojoSimpleParam;

  }

  public static class ComplexPojo {

    @Parameter
    private SimplePojo simple;

  }

  public static abstract class AbstractPojo {

    @Parameter
    private String pojoSuperclassParam;

  }

  public static class ConcreteComplexPojo extends AbstractPojo {

    @Parameter
    private SimplePojo simpleSubclass;

  }

  public class ParameterModelMatcher extends BaseMatcher<ParameterModel> {

    private final String paramName;

    public ParameterModelMatcher(String paramName) {
      this.paramName = paramName;
    }

    @Override
    public boolean matches(Object item) {
      if (item instanceof ParameterModel) {
        return ((ParameterModel) item).getName().equals(paramName);
      } else {
        return false;
      }
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(paramName);
    }

  }

  public class ParameterRawValueModelMatcher extends BaseMatcher<ComponentParameterAst> {

    private final String rawParamValue;

    public ParameterRawValueModelMatcher(String rawParamValue) {
      this.rawParamValue = rawParamValue;
    }

    @Override
    public boolean matches(Object item) {
      if (item instanceof ComponentParameterAst) {
        return ((ComponentParameterAst) item).getRawValue().equals(rawParamValue);
      } else {
        return false;
      }
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(rawParamValue);
    }

  }

  public class ParameterNestedValueModelMatcher extends BaseMatcher<ComponentParameterAst> {

    private final ComponentAst nestedParamValue;

    public ParameterNestedValueModelMatcher(ComponentAst nestedParamValue) {
      this.nestedParamValue = nestedParamValue;
    }

    @Override
    public boolean matches(Object item) {
      if (item instanceof ComponentParameterAst) {
        return ((ComponentParameterAst) item).getValue().getRight().equals(nestedParamValue);
      } else {
        return false;
      }
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(nestedParamValue.toString());
    }

  }

}
