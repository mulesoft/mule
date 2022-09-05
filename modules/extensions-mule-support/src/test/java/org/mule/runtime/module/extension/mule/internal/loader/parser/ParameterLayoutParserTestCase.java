/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.Utils.singleParameterAst;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.PARAMETERS;

import static java.util.stream.Stream.of;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.meta.model.display.PathModel;
import org.mule.runtime.api.meta.model.display.PathModel.Location;
import org.mule.runtime.api.meta.model.display.PathModel.Type;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.tck.junit4.AbstractMuleTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(REUSE)
@Story(PARAMETERS)
public class ParameterLayoutParserTestCase extends AbstractMuleTestCase {

  @Test
  public void whenTheParameterAstHasNotMetadataThenParsedLayoutModelAndDisplayModelAreNotPresent() {
    ComponentAst parameterAst = emptyParameterAst();
    ParameterLayoutParser parser = new ParameterLayoutParser(parameterAst);

    assertThat(parser.getLayoutModel().isPresent(), is(false));
    assertThat(parser.getDisplayModel().isPresent(), is(false));
  }

  @Test
  public void whenTheParameterMetadataIsEmptyThenParsedLayoutModelAndDisplayModelAreNotPresent() {
    ComponentAst parameterAst = mockParameterWithLayoutAst(new MockLayoutAstBuilder().build());
    ParameterLayoutParser parser = new ParameterLayoutParser(parameterAst);

    assertThat(parser.getLayoutModel().isPresent(), is(false));
    assertThat(parser.getDisplayModel().isPresent(), is(false));
  }

  @Test
  public void setDisplayNameOnly() {
    ComponentAst parameterAst =
        mockParameterWithLayoutAst(new MockLayoutAstBuilder().withDisplayName("The display name").build());
    ParameterLayoutParser parser = new ParameterLayoutParser(parameterAst);

    assertThat(parser.getLayoutModel().isPresent(), is(false));
    assertThat(parser.getDisplayModel().isPresent(), is(true));
    assertThat(parser.getDisplayModel().get().getDisplayName(), is("The display name"));
  }

  @Test
  public void setExampleOnly() {
    ComponentAst parameterAst = mockParameterWithLayoutAst(new MockLayoutAstBuilder().withExample("The Example").build());
    ParameterLayoutParser parser = new ParameterLayoutParser(parameterAst);

    assertThat(parser.getLayoutModel().isPresent(), is(false));
    assertThat(parser.getDisplayModel().isPresent(), is(true));
    assertThat(parser.getDisplayModel().get().getExample(), is("The Example"));
  }

  @Test
  public void setSummaryOnly() {
    ComponentAst parameterAst = mockParameterWithLayoutAst(new MockLayoutAstBuilder().withSummary("The Summary").build());
    ParameterLayoutParser parser = new ParameterLayoutParser(parameterAst);

    assertThat(parser.getLayoutModel().isPresent(), is(false));
    assertThat(parser.getDisplayModel().isPresent(), is(true));
    assertThat(parser.getDisplayModel().get().getSummary(), is("The Summary"));
  }

  @Test
  public void setPathWithoutParams() {
    ComponentAst parameterAst = mockParameterWithLayoutAst(new MockLayoutAstBuilder()
        .withPathAst(new MockPathAstBuilder().build())
        .build());
    ParameterLayoutParser parser = new ParameterLayoutParser(parameterAst);

    assertThat(parser.getLayoutModel().isPresent(), is(false));
    assertThat(parser.getDisplayModel().isPresent(), is(true));
    assertThat(parser.getDisplayModel().get().getPathModel().isPresent(), is(true));

    PathModel pathModel = parser.getDisplayModel().get().getPathModel().get();
    assertThat(pathModel.getType(), is(Type.ANY));
    assertThat(pathModel.acceptsUrls(), is(false));
    assertThat(pathModel.getLocation(), is(Location.ANY));
    assertThat(pathModel.getFileExtensions(), is(empty()));
  }

  @Test
  public void setPathWithParams() {
    ComponentAst parameterAst = mockParameterWithLayoutAst(new MockLayoutAstBuilder()
        .withPathAst(new MockPathAstBuilder()
            .withType("FILE")
            .acceptsUrls(true)
            .withLocation("EMBEDDED")
            .withAcceptedFileExtensions("xml,json")
            .build())
        .build());
    ParameterLayoutParser parser = new ParameterLayoutParser(parameterAst);

    assertThat(parser.getLayoutModel().isPresent(), is(false));
    assertThat(parser.getDisplayModel().isPresent(), is(true));
    assertThat(parser.getDisplayModel().get().getPathModel().isPresent(), is(true));

    PathModel pathModel = parser.getDisplayModel().get().getPathModel().get();
    assertThat(pathModel.getType(), is(Type.FILE));
    assertThat(pathModel.acceptsUrls(), is(true));
    assertThat(pathModel.getLocation(), is(Location.EMBEDDED));
    assertThat(pathModel.getFileExtensions(), containsInAnyOrder("xml", "json"));
  }

  @Test
  public void directoryPathTypeIgnoresFileExtensions() {
    ComponentAst parameterAst = mockParameterWithLayoutAst(new MockLayoutAstBuilder()
        .withPathAst(new MockPathAstBuilder()
            .withType("DIRECTORY")
            .withAcceptedFileExtensions("xml,json")
            .build())
        .build());
    ParameterLayoutParser parser = new ParameterLayoutParser(parameterAst);

    assertThat(parser.getLayoutModel().isPresent(), is(false));
    assertThat(parser.getDisplayModel().isPresent(), is(true));
    assertThat(parser.getDisplayModel().get().getPathModel().isPresent(), is(true));

    PathModel pathModel = parser.getDisplayModel().get().getPathModel().get();
    assertThat(pathModel.getType(), is(Type.DIRECTORY));
    assertThat(pathModel.acceptsUrls(), is(false));
    assertThat(pathModel.getLocation(), is(Location.ANY));
    assertThat(pathModel.getFileExtensions(), empty());
  }

  @Test
  public void setTextTrueOnly() {
    ComponentAst parameterAst = mockParameterWithLayoutAst(new MockLayoutAstBuilder().withText(true).build());
    ParameterLayoutParser parser = new ParameterLayoutParser(parameterAst);

    assertThat(parser.getDisplayModel().isPresent(), is(false));
    assertThat(parser.getLayoutModel().isPresent(), is(true));
    assertThat(parser.getLayoutModel().get().isText(), is(true));
  }

  @Test
  public void setTextFalseOnly() {
    ComponentAst parameterAst = mockParameterWithLayoutAst(new MockLayoutAstBuilder().withText(false).build());
    ParameterLayoutParser parser = new ParameterLayoutParser(parameterAst);

    assertThat(parser.getDisplayModel().isPresent(), is(false));
    assertThat(parser.getLayoutModel().isPresent(), is(true));
    assertThat(parser.getLayoutModel().get().isText(), is(false));
  }

  @Test
  public void setSecretOnly() {
    ComponentAst parameterAst = mockParameterWithLayoutAst(new MockLayoutAstBuilder().withSecret("CLIENT_SECRET").build());
    ParameterLayoutParser parser = new ParameterLayoutParser(parameterAst);

    assertThat(parser.getDisplayModel().isPresent(), is(false));
    assertThat(parser.getLayoutModel().isPresent(), is(true));
    assertThat(parser.getLayoutModel().get().isPassword(), is(true));
    assertThat(parser.getSemanticTerms(), containsInAnyOrder("connectivity.clientSecret"));
  }

  @Test
  public void setOrderOnly() {
    ComponentAst parameterAst = mockParameterWithLayoutAst(new MockLayoutAstBuilder().withOrder(3).build());
    ParameterLayoutParser parser = new ParameterLayoutParser(parameterAst);

    assertThat(parser.getDisplayModel().isPresent(), is(false));
    assertThat(parser.getLayoutModel().isPresent(), is(true));
    assertThat(parser.getLayoutModel().get().getOrder().isPresent(), is(true));
    assertThat(parser.getLayoutModel().get().getOrder().get(), is(3));
  }

  private static ComponentAst emptyParameterAst() {
    return mock(ComponentAst.class);
  }

  private static ComponentAst mockParameterWithLayoutAst(ComponentAst layoutAst) {
    ComponentAst parameterAst = mock(ComponentAst.class);
    when(parameterAst.directChildrenStreamByIdentifier(null, "parameter-metadata")).thenReturn(of(layoutAst));
    return parameterAst;
  }

  private static class MockLayoutAstBuilder {

    private final ComponentAst layoutAst;

    MockLayoutAstBuilder() {
      layoutAst = mock(ComponentAst.class);
    }

    MockLayoutAstBuilder withDisplayName(String displayName) {
      ComponentParameterAst displayNameAst = singleParameterAst(displayName);
      when(layoutAst.getParameter(DEFAULT_GROUP_NAME, "displayName")).thenReturn(displayNameAst);
      return this;
    }

    MockLayoutAstBuilder withExample(String example) {
      ComponentParameterAst exampleAst = singleParameterAst(example);
      when(layoutAst.getParameter(DEFAULT_GROUP_NAME, "example")).thenReturn(exampleAst);
      return this;
    }

    MockLayoutAstBuilder withSummary(String summary) {
      ComponentParameterAst summaryAst = singleParameterAst(summary);
      when(layoutAst.getParameter(DEFAULT_GROUP_NAME, "summary")).thenReturn(summaryAst);
      return this;
    }

    MockLayoutAstBuilder withPathAst(ComponentAst pathAst) {
      when(layoutAst.directChildrenStreamByIdentifier(null, "path")).thenReturn(of(pathAst));
      return this;
    }

    MockLayoutAstBuilder withText(boolean isText) {
      ComponentParameterAst isTextAst = singleParameterAst(isText);
      when(layoutAst.getParameter(DEFAULT_GROUP_NAME, "text")).thenReturn(isTextAst);
      return this;
    }

    MockLayoutAstBuilder withSecret(String secret) {
      ComponentParameterAst secretAst = singleParameterAst(secret);
      when(layoutAst.getParameter(DEFAULT_GROUP_NAME, "secret")).thenReturn(secretAst);
      return this;
    }

    MockLayoutAstBuilder withOrder(Integer order) {
      ComponentParameterAst orderAst = singleParameterAst(order);
      when(layoutAst.getParameter(DEFAULT_GROUP_NAME, "order")).thenReturn(orderAst);
      return this;
    }

    ComponentAst build() {
      return layoutAst;
    }
  }

  private static class MockPathAstBuilder {

    private final ComponentAst pathAst;

    MockPathAstBuilder() {
      pathAst = mock(ComponentAst.class);
    }

    MockPathAstBuilder withType(String pathType) {
      ComponentParameterAst typeAst = singleParameterAst(pathType);
      when(pathAst.getParameter(DEFAULT_GROUP_NAME, "type")).thenReturn(typeAst);
      return this;
    }

    MockPathAstBuilder acceptsUrls(Boolean acceptsUrls) {
      ComponentParameterAst acceptsUrlsAst = singleParameterAst(acceptsUrls);
      when(pathAst.getParameter(DEFAULT_GROUP_NAME, "acceptsUrls")).thenReturn(acceptsUrlsAst);
      return this;
    }

    MockPathAstBuilder withLocation(String location) {
      ComponentParameterAst locationAst = singleParameterAst(location);
      when(pathAst.getParameter(DEFAULT_GROUP_NAME, "location")).thenReturn(locationAst);
      return this;
    }

    MockPathAstBuilder withAcceptedFileExtensions(String acceptedFileExtensions) {
      ComponentParameterAst acceptedFileExtensionsAst = singleParameterAst(acceptedFileExtensions);
      when(pathAst.getParameter(DEFAULT_GROUP_NAME, "acceptedFileExtensions")).thenReturn(acceptedFileExtensionsAst);
      return this;
    }

    ComponentAst build() {
      return pathAst;
    }
  }
}
