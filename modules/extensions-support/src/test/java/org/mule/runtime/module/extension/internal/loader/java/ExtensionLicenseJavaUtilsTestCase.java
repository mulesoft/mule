/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getRequiresEnterpriseLicenseInfo;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.getRequiresEntitlementInfo;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.annotation.license.RequiresEnterpriseLicense;
import org.mule.runtime.extension.api.annotation.license.RequiresEntitlement;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ClassBasedAnnotationValueFetcher;
import org.mule.runtime.module.extension.internal.loader.parser.java.info.RequiresEnterpriseLicenseInfo;
import org.mule.runtime.module.extension.internal.loader.parser.java.info.RequiresEntitlementInfo;

import java.lang.annotation.Annotation;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ExtensionLicenseJavaUtilsTestCase {

  private static final String ENTITLEMENT_NAME = "Entitlement name";
  private static final String ENTITLEMENT_DESCRIPTION = "Entitlement description";
  private static final boolean ENTREPRISE_LICENSE_ALLOWS_EVALUATION = true;

  private ExtensionElement extensionElementMock = mock(ExtensionElement.class);
  private ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  @Rule
  public ExpectedException expectedException = none();

  @Before
  public void setUp() {
    reset(extensionElementMock);
  }

  @Test
  public void getRequireEnterpriseLicenseInfoFromLegacyAnnotation() {
    mockRequiresEnterpriseLicenseSdkAnnotationAbsent();
    mockRequiresEnterpriseLicenseAnnotationPresent();
    assertRequiresEnterpriseLicenseInfo(getRequiresEnterpriseLicenseInfo(extensionElementMock));
  }

  @Test
  public void getRequireEnterpriseLicenseInfoFromSdkAnnotation() {
    mockRequiresEnterpriseLicenseSdkAnnotationPresent();
    mockRequiresEnterpriseLicenseAnnotationAbsent();
    assertRequiresEnterpriseLicenseInfo(getRequiresEnterpriseLicenseInfo(extensionElementMock));
  }

  @Test
  public void getRequireEnterpriseLicenseInfoWhenBothAnnotationsArePresent() {
    mockRequiresEnterpriseLicenseAnnotationPresent();
    mockRequiresEnterpriseLicenseSdkAnnotationPresent();

    expectedException.expect(IllegalParameterModelDefinitionException.class);
    getRequiresEnterpriseLicenseInfo(extensionElementMock);
  }

  @Test
  public void getRequireEnterpriseLicenseInfoWhenNotPresent() {
    mockRequiresEnterpriseLicenseAnnotationAbsent();
    mockRequiresEnterpriseLicenseSdkAnnotationAbsent();
    assertThat(getRequiresEnterpriseLicenseInfo(extensionElementMock).isPresent(), is(false));
  }

  @Test
  public void getRequiresEntitlementInfoFromLegacyAnnotation() {
    mockRequiresEntitlementAnnotationPresent();
    mockRequiresEntitlementSdkAnnotationAbsent();
    assertRequiresEntitlementInfo(getRequiresEntitlementInfo(extensionElementMock));
  }

  @Test
  public void getRequiresEntitlementLicenseInfoFromSdkAnnotation() {
    mockRequiresEntitlementSdkAnnotationPresent();
    mockRequiresEntitlementAnnotationAbsent();
    assertRequiresEntitlementInfo(getRequiresEntitlementInfo(extensionElementMock));
  }

  @Test
  public void getRequiresEntitlementInfoWhenBothAnnotationsArePesent() {
    mockRequiresEntitlementAnnotationPresent();
    mockRequiresEntitlementSdkAnnotationPresent();

    expectedException.expect(IllegalParameterModelDefinitionException.class);
    getRequiresEntitlementInfo(extensionElementMock);
  }

  @Test
  public void getRequiresEntitlementInfoWhenNotPresent() {
    mockRequiresEntitlementSdkAnnotationAbsent();
    mockRequiresEntitlementAnnotationAbsent();
    assertThat(getRequiresEntitlementInfo(extensionElementMock).isPresent(), is(false));
  }

  private void assertRequiresEnterpriseLicenseInfo(Optional<RequiresEnterpriseLicenseInfo> requiresEnterpriseLicenseInfo) {
    assertThat(requiresEnterpriseLicenseInfo.isPresent(), is(true));
    RequiresEnterpriseLicenseInfo requiresEnterpriseLicenseInfoObject = requiresEnterpriseLicenseInfo.get();

    assertThat(requiresEnterpriseLicenseInfoObject.isAllowEvaluationLicense(), is(ENTREPRISE_LICENSE_ALLOWS_EVALUATION));
  }


  private void assertRequiresEntitlementInfo(Optional<RequiresEntitlementInfo> requiresEntitlementInfo) {
    assertThat(requiresEntitlementInfo.isPresent(), is(true));
    RequiresEntitlementInfo requiresEntitlementInfoObject = requiresEntitlementInfo.get();

    assertThat(requiresEntitlementInfoObject.getName(), is(ENTITLEMENT_NAME));
    assertThat(requiresEntitlementInfoObject.getDescription(), is(ENTITLEMENT_DESCRIPTION));
  }

  private void mockRequiresEntitlementAnnotationPresent() {
    when(extensionElementMock.getValueFromAnnotation(RequiresEntitlement.class))
        .thenReturn(of(new ClassBasedAnnotationValueFetcher<>(new RequiresEntitlement() {

          @Override
          public String name() {
            return ENTITLEMENT_NAME;
          }

          @Override
          public String description() {
            return ENTITLEMENT_DESCRIPTION;
          }

          @Override
          public Class<? extends Annotation> annotationType() {
            return RequiresEntitlement.class;
          }
        }, typeLoader)));
  }

  private void mockRequiresEntitlementAnnotationAbsent() {
    when(extensionElementMock.getValueFromAnnotation(RequiresEntitlement.class)).thenReturn(empty());
  }

  private void mockRequiresEnterpriseLicenseAnnotationPresent() {
    when(extensionElementMock.getValueFromAnnotation(RequiresEnterpriseLicense.class)).thenReturn(
                                                                                                  of(new ClassBasedAnnotationValueFetcher<>(new RequiresEnterpriseLicense() {

                                                                                                    @Override
                                                                                                    public boolean allowEvaluationLicense() {
                                                                                                      return ENTREPRISE_LICENSE_ALLOWS_EVALUATION;
                                                                                                    }

                                                                                                    @Override
                                                                                                    public Class<? extends Annotation> annotationType() {
                                                                                                      return RequiresEnterpriseLicense.class;
                                                                                                    }
                                                                                                  }, typeLoader)));
  }

  private void mockRequiresEnterpriseLicenseAnnotationAbsent() {
    when(extensionElementMock.getAnnotation(RequiresEnterpriseLicense.class)).thenReturn(empty());
  }

  private void mockRequiresEntitlementSdkAnnotationPresent() {
    when(extensionElementMock.getValueFromAnnotation(org.mule.sdk.api.annotation.license.RequiresEntitlement.class))
        .thenReturn(of(new ClassBasedAnnotationValueFetcher<>(new org.mule.sdk.api.annotation.license.RequiresEntitlement() {

          @Override
          public String name() {
            return ENTITLEMENT_NAME;
          }

          @Override
          public String description() {
            return ENTITLEMENT_DESCRIPTION;
          }

          @Override
          public Class<? extends Annotation> annotationType() {
            return org.mule.sdk.api.annotation.license.RequiresEntitlement.class;
          }
        }, typeLoader)));
  }

  private void mockRequiresEntitlementSdkAnnotationAbsent() {
    when(extensionElementMock.getAnnotation(org.mule.sdk.api.annotation.license.RequiresEntitlement.class)).thenReturn(empty());
  }

  private void mockRequiresEnterpriseLicenseSdkAnnotationPresent() {
    when(extensionElementMock.getValueFromAnnotation(org.mule.sdk.api.annotation.license.RequiresEnterpriseLicense.class))
        .thenReturn(of(new ClassBasedAnnotationValueFetcher<>(new org.mule.sdk.api.annotation.license.RequiresEnterpriseLicense() {

          @Override
          public boolean allowEvaluationLicense() {
            return ENTREPRISE_LICENSE_ALLOWS_EVALUATION;
          }

          @Override
          public Class<? extends Annotation> annotationType() {
            return org.mule.sdk.api.annotation.license.RequiresEnterpriseLicense.class;
          }
        }, typeLoader)));
  }

  private void mockRequiresEnterpriseLicenseSdkAnnotationAbsent() {
    when(extensionElementMock.getValueFromAnnotation(org.mule.sdk.api.annotation.license.RequiresEnterpriseLicense.class))
        .thenReturn(empty());
  }

}
