/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.lang.String.format;
import static org.mule.runtime.extension.api.dsql.DsqlParser.isDsqlQuery;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.dsql.DsqlParser;
import org.mule.runtime.extension.api.dsql.DsqlQuery;
import org.mule.runtime.extension.api.dsql.QueryTranslator;

/**
 * {@link ValueResolver} implementation which translates {@link DsqlQuery}s to queries in the Native Query Language.
 * <p>
 * If the query provided is not a {@link DsqlQuery} then is considered a Native Query and returned as it is.
 *
 * @since 4.0
 */
public final class NativeQueryParameterValueResolver extends AbstractValueResolverWrapper<String> {

  private static final DsqlParser dsqlParser = DsqlParser.getInstance();
  private final static String ERROR = "Error creating QueryTranslator [%s], query translators must have a default constructor";

  private final Class<? extends QueryTranslator> translatorClass;

  public NativeQueryParameterValueResolver(ValueResolver<String> queryResolver, Class<? extends QueryTranslator> translator) {
    super(queryResolver);
    this.translatorClass = translator;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String resolve(ValueResolvingContext context) throws MuleException {
    String query = super.resolve(context);

    if (!isDsqlQuery(query)) {
      return query;
    }

    DsqlQuery dsqlQuery = dsqlParser.parse(query);
    // creates new instances to avoid state related problems of the translator implementation.
    QueryTranslator queryTranslator = instantiateTranslator(translatorClass);
    return dsqlQuery.translate(queryTranslator);
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isDynamic() {
    return false;
  }

  private QueryTranslator instantiateTranslator(Class<? extends QueryTranslator> translator) {
    try {
      return translator.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new IllegalArgumentException(format(ERROR, translator.getSimpleName()));
    }
  }
}
