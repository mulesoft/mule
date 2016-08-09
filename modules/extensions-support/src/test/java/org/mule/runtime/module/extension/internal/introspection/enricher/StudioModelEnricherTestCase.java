/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.runtime.extension.api.annotation.capability.CustomStudioEditor;
import org.mule.runtime.extension.api.annotation.capability.StudioProvidedEditor;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.spi.ModelEnricher;
import org.mule.runtime.extension.api.introspection.property.StudioModelProperty;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.model.property.ImplementingTypeModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class StudioModelEnricherTestCase extends AbstractMuleTestCase {

  private static final String EXTENSION_STUDIO_EDITOR_FILE_NAME = "custom.xml";

  private ExtensionDeclarer extensionDeclarer = new ExtensionDeclarer();
  private ModelEnricher modelEnricher = new StudioModelEnricher();

  @Test
  public void enrich() {
    StudioModelProperty studioProperty = enrich(StudioSupport.class);

    assertExpectedValuesAre(studioProperty, "", true);
  }

  @Test
  public void enrichWithCustomEditor() {
    StudioModelProperty studioProperty = enrich(DefaultStudioExtension.class);

    assertExpectedValuesAre(studioProperty, EXTENSION_STUDIO_EDITOR_FILE_NAME, false);
  }

  @Test
  public void enrichWithProvidedEditor() {
    StudioModelProperty studioProperty = enrich(DefaultStudioExtensionWithProvidedEditor.class);

    assertExpectedValuesAre(studioProperty, "", false);
  }

  private StudioModelProperty enrich(Class<?> type) {
    extensionDeclarer.withModelProperty(new ImplementingTypeModelProperty(type));
    modelEnricher.enrich(new DefaultDescribingContext(extensionDeclarer, type.getClassLoader()));
    return extensionDeclarer.getDeclaration().getModelProperty(StudioModelProperty.class).get();
  }

  private void assertExpectedValuesAre(StudioModelProperty studioProperty, String expectedEditor, boolean expectedDerived) {
    assertThat(studioProperty, is(notNullValue()));
    assertThat(studioProperty.getEditorFileName(), is(expectedEditor));
    assertThat(studioProperty.isDerived(), is(expectedDerived));
  }

  private static class StudioSupport {

  }

  @CustomStudioEditor(fileName = EXTENSION_STUDIO_EDITOR_FILE_NAME)
  private static class DefaultStudioExtension {

  }

  @StudioProvidedEditor
  private static class DefaultStudioExtensionWithProvidedEditor {

  }
}
