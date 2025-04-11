/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.dsl;

import static org.mule.runtime.extension.api.util.XmlModelUtils.buildSchemaLocation;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.component.ComponentIdentifier;

/**
 * Constants for Mule SDK DSL.
 *
 * @since 4.5
 */
@NoImplement
public interface MuleSdkDslConstants {

  public static final String MULE_SDK_EXTENSION_DSL_NAMESPACE_URI = "http://www.mulesoft.org/schema/mule/mule-extension";
  public static final String MULE_SDK_EXTENSION_DSL_NAMESPACE = "extension";

  String MULE_SDK_EXTENSION_DESCRIPTION_CONSTRUCT_NAME = "description";
  String MULE_SDK_EXTENSION_NAME_PARAMETER_NAME = "name";
  String MULE_SDK_EXTENSION_CATEGORY_PARAMETER_NAME = "category";
  String MULE_SDK_EXTENSION_VENDOR_PARAMETER_NAME = "vendor";
  String MULE_SDK_EXTENSION_LICENSING_COMPONENT_NAME = "licensing";
  String MULE_SDK_EXTENSION_REQUIRED_ENTITLEMENT_PARAMETER_NAME = "requiredEntitlement";
  String MULE_SDK_EXTENSION_REQUIRES_ENTERPRISE_LICENSE_PARAMETER_NAME = "requiresEnterpriseLicense";
  String MULE_SDK_EXTENSION_ALLOWS_EVALUATION_LICENSE_PARAMETER_NAME = "allowsEvaluationLicense";
  String MULE_SDK_EXTENSION_XML_DSL_ATTRIBUTES_COMPONENT_NAME = "xml-dsl-attributes";
  String MULE_SDK_EXTENSION_NAMESPACE_PARAMETER_NAME = "namespace";
  String MULE_SDK_EXTENSION_PREFIX_PARAMETER_NAME = "prefix";

  String MULE_SDK_EXTENSION_DSL_ERRORS_CONSTRUCT_NAME = "errors";
  String MULE_SDK_EXTENSION_DSL_ERROR_CONSTRUCT_NAME = "error";
  String MULE_SDK_EXTENSION_DSL_XSD_FILE_NAME = "mule-extension.xsd";
  String MULE_SDK_EXTENSION_DSL_SCHEMA_LOCATION =
      buildSchemaLocation(MULE_SDK_EXTENSION_DSL_NAMESPACE, MULE_SDK_EXTENSION_DSL_XSD_FILE_NAME);

  ComponentIdentifier MULE_SDK_EXTENSION_DESCRIPTION_IDENTIFIER = ComponentIdentifier.builder()
      .namespace(MULE_SDK_EXTENSION_DSL_NAMESPACE)
      .namespaceUri(MULE_SDK_EXTENSION_DSL_NAMESPACE_URI)
      .name(MULE_SDK_EXTENSION_DESCRIPTION_CONSTRUCT_NAME)
      .build();
  ComponentIdentifier MULE_SDK_EXTENSION_DSL_ERRORS_CONSTRUCT_IDENTIFIER = ComponentIdentifier.builder()
      .namespace(MULE_SDK_EXTENSION_DSL_NAMESPACE)
      .name(MULE_SDK_EXTENSION_DSL_ERRORS_CONSTRUCT_NAME)
      .build();
}
