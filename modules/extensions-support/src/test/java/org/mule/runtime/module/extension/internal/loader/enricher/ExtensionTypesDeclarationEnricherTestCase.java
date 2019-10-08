/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getType;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.loadExtension;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.declaration.type.annotation.InfrastructureTypeAnnotation;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.model.*;
import org.mule.test.heisenberg.extension.model.drugs.Drug;
import org.mule.test.heisenberg.extension.model.drugs.Meta;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.qameta.allure.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.mule.test.heisenberg.extension.model.types.DEAOfficerAttributes;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ExtensionTypesDeclarationEnricherTestCase extends AbstractMuleTestCase {

  private ExtensionModel extensionModel;

  @Before
  public void before() {
    extensionModel = loadExtension(HeisenbergExtension.class);
  }

  @Test
  public void assertTypes() throws Exception {
    assertTypes(extensionModel.getTypes(), true, "Type %s was not present",
                Ricin.class, KnockeableDoor.class, CarWash.class,
                Weapon.class, Weapon.WeaponAttributes.class, PersonalInfo.class, Methylamine.class);

    assertTypes(extensionModel.getTypes(), false, "Invalid type %s was exported",
                Object.class, Map.class);
  }

  private void assertTypes(Set<ObjectType> extensionTypes, boolean typeShouldBePresent, String message,
                           Class<?>... expectedTypes) {
    for (Class<?> expectedType : expectedTypes) {
      Optional<ObjectType> extensionType = extensionTypes.stream()
          .filter(type -> getType(type).map(expectedType::equals).orElse(false))
          .findFirst();

      assertThat(format(message, expectedType.getName()), extensionType.isPresent(), is(typeShouldBePresent));
    }
  }

  @Test
  public void noInfrastructureTypes() throws Exception {
    extensionModel.getTypes()
        .forEach(type -> assertThat(type.getAnnotation(InfrastructureTypeAnnotation.class).isPresent(), is(false)));
  }

  @Test
  @Description("Checks that types that are declared in the extension but not used explicitly are added")
  public void addsUnusedDeclaredTypes() throws Exception {
    assertTypes(extensionModel.getTypes(), true, "Type %s was not present", Drug.class, Meta.class);
  }

  @Test
  @Description("Checks that POJOs declared in structures like PagingProvider<Connnection C,Result<POJO,Void>> or List<Result<Void,POJO>> are added as Types in the model.")
  public void addsPOJOsInsideAListOfResultsAsTypes() throws Exception {
    assertTypes(extensionModel.getTypes(), true, "Type %s was not present", DrugBatch.class, DEAOfficerAttributes.class);
  }
}
