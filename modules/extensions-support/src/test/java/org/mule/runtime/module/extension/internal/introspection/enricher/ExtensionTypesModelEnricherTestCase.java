/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.extension.internal.util.MetadataTypeUtils.getId;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.exception.HeisenbergException;
import org.mule.test.heisenberg.extension.model.CarWash;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;
import org.mule.test.heisenberg.extension.model.PersonalInfo;
import org.mule.test.heisenberg.extension.model.Ricin;
import org.mule.test.heisenberg.extension.model.Weapon;

import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ExtensionTypesModelEnricherTestCase extends AbstractMuleTestCase {

  private ExtensionModel extensionModel;

  @Before
  public void before() {
    final DefaultDescribingContext describingContext = new DefaultDescribingContext(getClass().getClassLoader());
    ExtensionDeclarer declarer =
        new AnnotationsBasedDescriber(HeisenbergExtension.class, new StaticVersionResolver("4.0")).describe(
                                                                                                            describingContext);
    extensionModel =
        new DefaultExtensionFactory(new SpiServiceRegistry(), getClass().getClassLoader()).createFrom(declarer,
                                                                                                      describingContext);
  }

  @Test
  public void assertTypes() throws Exception {
    doAssertTypes(extensionModel.getTypes(), Ricin.class, KnockeableDoor.class, HeisenbergException.class, CarWash.class,
                  Weapon.class, Weapon.WeaponAttributes.class, PersonalInfo.class);
  }

  private void doAssertTypes(Set<ObjectType> extensionTypes, Class<?>... expectedTypes) {
    for (Class<?> expectedType : expectedTypes) {
      Optional<ObjectType> extensionType = extensionTypes.stream()
          .filter(type -> getId(type).equals(expectedType.getCanonicalName()))
          .findFirst();

      assertThat(format("Type %s was not present", expectedType.getName()), extensionType.isPresent(), is(true));
    }
  }
}
