/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.parameter;

import static org.mule.runtime.dsl.api.component.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.dsl.api.component.TypeDefinition.fromType;

import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition.Builder;
import org.mule.runtime.extension.api.dsl.syntax.DslElementSyntax;
import org.mule.runtime.extension.api.dsl.syntax.resolver.DslSyntaxResolver;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.config.dsl.ExtensionParsingContext;

import java.util.Map;

/**
 * A {@link ParameterGroupParser} which returns the values of the parameters in the group
 * as a {@link Map}
 *
 * @since 4.0
 */
public class AnonymousInlineParameterGroupParser extends ParameterGroupParser {

  public AnonymousInlineParameterGroupParser(Builder definition,
                                             ParameterGroupModel group,
                                             ClassLoader classLoader,
                                             DslElementSyntax groupDsl,
                                             DslSyntaxResolver dslResolver,
                                             ExtensionParsingContext context) {
    super(definition, group, classLoader, groupDsl, dslResolver, context);
  }

  @Override
  protected Builder doParse(Builder definitionBuilder) throws ConfigurationException {
    Builder finalBuilder =
        definitionBuilder.withIdentifier(name).withNamespace(namespace).asNamed().withTypeDefinition(fromType(Map.class))
            .withObjectFactoryType(AnonymousGroupObjectFactory.class)
            .withConstructorParameterDefinition(fromReferenceObject(MuleContext.class).build());

    this.parseParameters(group.getParameterModels());

    return finalBuilder;
  }

  public static class AnonymousGroupObjectFactory extends AbstractExtensionObjectFactory<Map> {

    public AnonymousGroupObjectFactory(MuleContext muleContext) {
      super(muleContext);
    }

    @Override
    public Map getObject() throws Exception {
      return parameters;
    }

    @Override
    public Map doGetObject() throws Exception {
      throw new UnsupportedOperationException("This factory returns a simple Java Map. We can't have annotations on a Map");
    }
  }

}
