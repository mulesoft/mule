/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl;

import static org.mule.runtime.api.component.ComponentIdentifier.builder;
import static org.mule.runtime.internal.dsl.DslConstants.CORE_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.DOMAIN_PREFIX;
import static org.mule.runtime.internal.dsl.DslConstants.EE_DOMAIN_PREFIX;
import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.component.ComponentIdentifier;

/**
 * Constants for Mule XML DSL.
 * 
 * @since 4.0
 */
@NoImplement
public interface CoreDslConstants {

  String MULE_ROOT_ELEMENT = "mule";
  String MULE_DOMAIN_ROOT_ELEMENT = "mule-domain";
  String IMPORT_ELEMENT = "import";
  String RAISE_ERROR = "raise-error";
  String FLOW_ELEMENT = "flow";
  String FLOW_REF_ELEMENT = "flow-ref";
  String SUBFLOW_ELEMENT = "sub-flow";
  String ERROR_HANDLER = "error-handler";
  String ON_ERROR_CONTINUE = "on-error-continue";
  String ON_ERROR_PROPAGATE = "on-error-propagate";
  String CONFIGURATION_ELEMENT = "configuration";
  String CHOICE_ELEMENT = "choice";
  String ROUTE_ELEMENT = "route";
  String UNTIL_SUCCESSFUL_ELEMENT = "until-successful";
  String TRY_ELEMENT = "try";
  String FOREACH_ELEMENT = "foreach";
  String SCATTER_GATHER_ELEMENT = "scatter-gather";

  ComponentIdentifier ERROR_HANDLER_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ERROR_HANDLER).build();
  ComponentIdentifier ON_ERROR_CONTINE_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ON_ERROR_CONTINUE).build();
  ComponentIdentifier ON_ERROR_PROPAGATE_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ON_ERROR_PROPAGATE).build();
  ComponentIdentifier RAISE_ERROR_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(RAISE_ERROR).build();
  ComponentIdentifier MULE_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(MULE_ROOT_ELEMENT).build();
  ComponentIdentifier MULE_DOMAIN_IDENTIFIER =
      builder().namespace(DOMAIN_PREFIX).name(MULE_DOMAIN_ROOT_ELEMENT).build();
  ComponentIdentifier MULE_EE_DOMAIN_IDENTIFIER =
      builder().namespace(EE_DOMAIN_PREFIX).name(MULE_DOMAIN_ROOT_ELEMENT).build();
  ComponentIdentifier FLOW_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(FLOW_ELEMENT).build();
  ComponentIdentifier FLOW_REF_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(FLOW_REF_ELEMENT).build();
  ComponentIdentifier SUBFLOW_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(SUBFLOW_ELEMENT).build();
  ComponentIdentifier CONFIGURATION_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(CONFIGURATION_ELEMENT).build();
  ComponentIdentifier CHOICE_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(CHOICE_ELEMENT).build();
  ComponentIdentifier ROUTE_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(ROUTE_ELEMENT).build();
  ComponentIdentifier UNTIL_SUCCESSFUL_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(UNTIL_SUCCESSFUL_ELEMENT).build();
  ComponentIdentifier TRY_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(TRY_ELEMENT).build();
  ComponentIdentifier FOREACH_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(FOREACH_ELEMENT).build();
  ComponentIdentifier SCATTER_GATHER_IDENTIFIER =
      builder().namespace(CORE_PREFIX).name(SCATTER_GATHER_ELEMENT).build();
}
