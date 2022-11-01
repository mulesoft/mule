/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.loader.parser;

import static org.mule.runtime.api.util.IdentifierParsingUtils.parseErrorType;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_DSL_ERRORS_CONSTRUCT_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_DSL_ERROR_CONSTRUCT_NAME;
import static org.mule.runtime.module.extension.mule.internal.dsl.MuleSdkDslConstants.MULE_SDK_EXTENSION_DSL_NAMESPACE;
import static org.mule.runtime.module.extension.mule.internal.loader.parser.Utils.mockErrorAst;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.EXTENSION_EXTENSION_MODEL;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.module.extension.internal.loader.parser.ErrorModelParser;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(REUSE)
@Stories({@Story(EXTENSION_EXTENSION_MODEL), @Story(ERROR_HANDLING)})
public class MuleSdkErrorsDeclarationParserTestCase extends AbstractMuleTestCase {

  private static final String TEST = "TEST";

  @Rule
  public ExpectedException expected = none();

  @Test
  public void extensionWithoutErrorsTag() {
    // Given an extension without <errors /> tag
    ComponentAst extensionComponentAst = mock(ComponentAst.class);
    when(extensionComponentAst.directChildrenStreamByIdentifier(MULE_SDK_EXTENSION_DSL_NAMESPACE,
                                                                MULE_SDK_EXTENSION_DSL_ERRORS_CONSTRUCT_NAME))
                                                                    .thenReturn(Stream.empty());

    // When we parse it.
    Map<ComponentIdentifier, ErrorModelParser> errorParsersMap =
        new MuleSdkErrorsDeclarationParser(extensionComponentAst, TEST).parse();

    // Then the resulting error parsers map is empty
    assertThat(errorParsersMap, is(emptyMap()));
  }

  @Test
  public void extensionWithEmptyErrorsTag() {
    // Given an extension with an empty <errors /> tag
    ComponentAst extensionComponentAst = mockExtensionAstWithErrors(/* empty */);

    // When we parse it.
    Map<ComponentIdentifier, ErrorModelParser> errorParsersMap =
        new MuleSdkErrorsDeclarationParser(extensionComponentAst, TEST).parse();

    // Then the resulting error parsers map is empty
    assertThat(errorParsersMap, is(emptyMap()));
  }

  @Test
  public void errorWithoutNamespace() {
    // Given an error without namespace:
    // <error type="WITHOUT_NS" />
    ComponentAst errorAst = mockErrorAst("WITHOUT_NS", null);
    ComponentAst extensionComponentAst = mockExtensionAstWithErrors(errorAst);

    // When we parse it.
    Map<ComponentIdentifier, ErrorModelParser> errorParsersMap =
        new MuleSdkErrorsDeclarationParser(extensionComponentAst, TEST).parse();

    // Then the resulting error has the extension's error namespace.
    ComponentIdentifier expectedErrorIdentifier = parseErrorType("WITHOUT_NS", TEST);
    assertThat(errorParsersMap, hasKey(expectedErrorIdentifier));
    ErrorModelParser errorModelParser = errorParsersMap.get(expectedErrorIdentifier);
    assertThat(errorModelParser.getNamespace(), is(TEST));
  }

  @Test
  public void errorWithNamespaceOfSameExtension() {
    // Given an error with the namespace of the same extension:
    // <error type="TEST:WITH_NS" />
    ComponentAst errorAst = mockErrorAst("TEST:WITH_NS", null);
    ComponentAst extensionComponentAst = mockExtensionAstWithErrors(errorAst);

    // When we parse it.
    Map<ComponentIdentifier, ErrorModelParser> errorParsersMap =
        new MuleSdkErrorsDeclarationParser(extensionComponentAst, TEST).parse();

    // Then the resulting error has the extension's error namespace.
    ComponentIdentifier expectedErrorIdentifier = parseErrorType("TEST:WITH_NS", TEST);
    assertThat(errorParsersMap, hasKey(expectedErrorIdentifier));
    ErrorModelParser errorModelParser = errorParsersMap.get(expectedErrorIdentifier);
    assertThat(errorModelParser.getNamespace(), is(TEST));
  }

  @Test
  public void errorWithNamespaceOtherThanTheExtensionOneIsForbidden() {
    // Given an error without namespace:
    // <error type="OTHER_THAN_TEST:WITH_NS" />
    ComponentAst errorAst = mockErrorAst("OTHER_THAN_TEST:WITH_NS", null);
    ComponentAst extensionComponentAst = mockExtensionAstWithErrors(errorAst);

    // When we parse it, we expect an exception.
    expected.expect(IllegalArgumentException.class);
    expected
        .expectMessage("The extension with namespace 'TEST' can't declare the error 'OTHER_THAN_TEST:WITH_NS' with namespace 'OTHER_THAN_TEST'");
    new MuleSdkErrorsDeclarationParser(extensionComponentAst, TEST).parse();
  }

  @Test
  public void parentWithNamespaceOtherThanTheExtensionOneOrMuleIsForbidden() {
    // Given an error with a parent that has namespace other than the extension one or mule:
    // <error type="CUSTOM_ERROR" parent="OTHER_THAN_TEST_OR_MULE:PARENT" />
    ComponentAst errorAst = mockErrorAst("CUSTOM_ERROR", "OTHER_THAN_TEST_OR_MULE:PARENT");
    ComponentAst extensionComponentAst = mockExtensionAstWithErrors(errorAst);

    // When we parse it, we expect an exception.
    expected.expect(IllegalArgumentException.class);
    expected
        .expectMessage("The error 'TEST:CUSTOM_ERROR' can't declare 'OTHER_THAN_TEST_OR_MULE:PARENT' as parent. It can only have a parent with namespace 'TEST' or 'MULE'");
    new MuleSdkErrorsDeclarationParser(extensionComponentAst, TEST).parse();
  }

  @Test
  public void errorWithParentFromCore() {
    // Given an error with a parent with namespace MULE:
    // <error type="CUSTOM_ERROR" parent="MULE:ANY" />
    ComponentAst errorAst = mockErrorAst("CUSTOM_ERROR", "MULE:PARENT");
    ComponentAst extensionComponentAst = mockExtensionAstWithErrors(errorAst);

    // When we parse it.
    Map<ComponentIdentifier, ErrorModelParser> errorParsersMap =
        new MuleSdkErrorsDeclarationParser(extensionComponentAst, TEST).parse();

    // Then the resulting error has a parent with such namespace.
    ComponentIdentifier expectedErrorIdentifier = parseErrorType("TEST:CUSTOM_ERROR", TEST);
    assertThat(errorParsersMap, hasKey(expectedErrorIdentifier));
    ErrorModelParser errorModelParser = errorParsersMap.get(expectedErrorIdentifier);
    Optional<ErrorModelParser> parentParser = errorModelParser.getParent();
    assertThat(parentParser.isPresent(), is(true));
    assertThat(parentParser.get().getNamespace(), is("MULE"));
    assertThat(parentParser.get().getType(), is("PARENT"));
  }

  @Test
  public void errorWithParentFromSameExtension() {
    // Given two errors where one is the parent of the other:
    // <error type="PARENT" />
    // <error type="CHILD" parent="PARENT" />
    ComponentAst parentErrorAst = mockErrorAst("PARENT", null);
    ComponentAst childErrorAst = mockErrorAst("CHILD", "PARENT");
    ComponentAst extensionComponentAst = mockExtensionAstWithErrors(childErrorAst, parentErrorAst);

    // When we parse it.
    Map<ComponentIdentifier, ErrorModelParser> errorParsersMap =
        new MuleSdkErrorsDeclarationParser(extensionComponentAst, TEST).parse();

    // Then the resulting error has a parent with such namespace.
    ComponentIdentifier parentErrorId = parseErrorType("TEST:PARENT", TEST);
    ComponentIdentifier childErrorId = parseErrorType("TEST:CHILD", TEST);
    assertThat(errorParsersMap, hasKey(parentErrorId));
    assertThat(errorParsersMap, hasKey(childErrorId));
    ErrorModelParser childParser = errorParsersMap.get(childErrorId);
    ErrorModelParser parentParser = errorParsersMap.get(parentErrorId);
    assertThat(childParser.getParent().get(), is(parentParser));
  }

  @Test
  public void circularDependency() {
    // Given two errors each is the parent of the other:
    // <error type="A" parent="B" />
    // <error type="B" parent="A" />
    ComponentAst aAst = mockErrorAst("A", "B");
    ComponentAst bAst = mockErrorAst("B", "A");
    ComponentAst extensionComponentAst = mockExtensionAstWithErrors(aAst, bAst);

    // When we parse it, we expect an exception.
    // TODO: Is this exception ok? It's pretty clear...
    expected.expect(IllegalArgumentException.class);
    expected.expectMessage("Graph is not a DAG");
    new MuleSdkErrorsDeclarationParser(extensionComponentAst, TEST).parse();
  }

  private static ComponentAst mockExtensionAstWithErrors(ComponentAst... errorsAsts) {
    ComponentAst extensionComponentAst = mock(ComponentAst.class);
    ComponentAst errorsComponentAst = mock(ComponentAst.class);
    when(extensionComponentAst.directChildrenStreamByIdentifier(MULE_SDK_EXTENSION_DSL_NAMESPACE,
            MULE_SDK_EXTENSION_DSL_ERRORS_CONSTRUCT_NAME))
            .thenReturn(Stream.of(errorsComponentAst));
    when(errorsComponentAst.directChildrenStreamByIdentifier(MULE_SDK_EXTENSION_DSL_NAMESPACE,
            MULE_SDK_EXTENSION_DSL_ERROR_CONSTRUCT_NAME))
            .thenReturn(Stream.of(errorsAsts));
    return extensionComponentAst;
  }
}
