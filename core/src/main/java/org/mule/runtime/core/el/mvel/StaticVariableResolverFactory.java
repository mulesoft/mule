/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.mvel;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.el.context.AppContext;
import org.mule.runtime.core.el.context.MuleInstanceContext;
import org.mule.runtime.core.el.context.ServerContext;
import org.mule.runtime.core.el.function.DateTimeExpressionLanguageFuntion;
import org.mule.runtime.core.el.function.RegexExpressionLanguageFuntion;
import org.mule.runtime.core.el.function.WildcardExpressionLanguageFuntion;
import org.mule.mvel2.ParserConfiguration;

public class StaticVariableResolverFactory extends MVELExpressionLanguageContext {

  private static final long serialVersionUID = -6819292692339684915L;

  public StaticVariableResolverFactory(ParserConfiguration parserConfiguration, MuleContext muleContext) {
    super(parserConfiguration, muleContext);
    addFinalVariable("server", new ServerContext());
    addFinalVariable("mule", new MuleInstanceContext(muleContext));
    addFinalVariable("app", new AppContext(muleContext));
    addFinalVariable(MVELExpressionLanguageContext.MULE_CONTEXT_INTERNAL_VARIABLE, muleContext);
    declareFunction("regex", new RegexExpressionLanguageFuntion());
    declareFunction("wildcard", new WildcardExpressionLanguageFuntion());
    declareFunction("dateTime", new DateTimeExpressionLanguageFuntion());
  }

}
