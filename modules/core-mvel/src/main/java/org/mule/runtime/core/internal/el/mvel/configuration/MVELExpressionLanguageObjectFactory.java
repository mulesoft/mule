/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el.mvel.configuration;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.el.mvel.MVELExpressionLanguage;
import org.mule.runtime.dsl.api.component.AbstractComponentFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * POJO for parsing the expression-language element.
 *
 * @since 4.0
 */
public class MVELExpressionLanguageObjectFactory extends AbstractComponentFactory<MVELExpressionLanguage> {

  @Inject
  MuleContext muleContext;

  private boolean autoResolveVariables;
  private MVELGlobalFunctionsConfig globalFunctions;
  private List<AliasEntry> aliases;
  private List<ImportEntry> imports;

  public void setAutoResolveVariables(boolean autoResolveVariables) {
    this.autoResolveVariables = autoResolveVariables;
  }

  public void setGlobalFunctions(MVELGlobalFunctionsConfig globalFunctions) {
    this.globalFunctions = globalFunctions;
  }

  public void setAliases(List<AliasEntry> aliases) {
    this.aliases = aliases;
  }

  public void setImports(List<ImportEntry> imports) {
    this.imports = imports;
  }

  @Override
  public MVELExpressionLanguage doGetObject() throws Exception {
    MVELExpressionLanguage result = new MVELExpressionLanguage(muleContext);

    result.setAutoResolveVariables(autoResolveVariables);

    if (globalFunctions != null) {
      result.setGlobalFunctionsFile(globalFunctions.getFile());
      result.setGlobalFunctionsString(globalFunctions.getInlineScript());
    }

    if (aliases != null) {
      Map<String, String> aliasesMap = new HashMap<>();
      aliases.forEach(x -> aliasesMap.put(x.getKey(), x.getValue()));
      result.setAliases(aliasesMap);
    }

    if (imports != null) {
      Map<String, Class<?>> importsMap = new HashMap<>();
      imports.forEach(x -> importsMap.put(x.getKey(), x.getValue()));
      result.setImports(importsMap);
    }

    return result;
  }
}
