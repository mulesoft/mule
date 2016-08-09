/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import static org.mule.runtime.module.extension.internal.introspection.enricher.EnricherTestUtils.getDeclaration;

import org.mule.runtime.extension.api.introspection.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.ParameterGroup;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.runtime.module.extension.internal.model.property.ParameterGroupModelProperty;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.model.ExtendedPersonalInfo;
import org.mule.test.heisenberg.extension.model.LifetimeInfo;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

public class ParameterGroupModelEnricherTestCase {

  private ExtensionDeclaration declaration;

  @Before
  public void setUp() {
    final AnnotationsBasedDescriber basedDescriber =
        new AnnotationsBasedDescriber(HeisenbergExtension.class, new StaticVersionResolver(getProductVersion()));
    ExtensionDeclarer declarer = basedDescriber.describe(new DefaultDescribingContext(getClass().getClassLoader()));
    new ParameterGroupModelEnricher().enrich(new DefaultDescribingContext(declarer, this.getClass().getClassLoader()));
    declaration = declarer.getDeclaration();
  }

  @Test
  public void verifyParameterGroupModelPropertyExistance() {
    final ConfigurationDeclaration config = getDeclaration(declaration.getConfigurations(), "config");

    final Optional<ParameterGroupModelProperty> modelProperty = config.getModelProperty(ParameterGroupModelProperty.class);
    assertThat(modelProperty.isPresent(), is(true));

    final List<ParameterGroup> groups = modelProperty.get().getGroups();
    assertThat(groups.size(), is(1));

    final ParameterGroup parameterGroup = groups.get(0);
    assertThat(parameterGroup.getParameters().size(), is(2));
    assertThat(parameterGroup.getType(), is(equalTo(ExtendedPersonalInfo.class)));

    final Optional<ParameterGroupModelProperty> childParamGroupProperty =
        parameterGroup.getModelProperty(ParameterGroupModelProperty.class);
    assertThat(childParamGroupProperty.isPresent(), is(true));

    final List<ParameterGroup> childGroups = childParamGroupProperty.get().getGroups();
    assertThat(childGroups.size(), is(1));

    final ParameterGroup lifeTimeInfoGroup = childGroups.get(0);
    assertThat(lifeTimeInfoGroup.getParameters().size(), is(3));
    assertThat(lifeTimeInfoGroup.getType(), is(equalTo(LifetimeInfo.class)));
  }
}
