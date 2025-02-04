/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.crafted.localisation.properties.extension;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.config.internal.model.dsl.properties.DefaultConfigurationPropertiesProvider;
import org.mule.runtime.properties.api.ConfigurationProperty;
import org.mule.runtime.properties.api.ResourceProvider;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;

public class LocalisationConfigurationPropertiesProvider extends DefaultConfigurationPropertiesProvider {

  private final static String LOCALE_PREFIX = "locale::";
  private final String language;
  private final String region;

  public LocalisationConfigurationPropertiesProvider(ResourceProvider resourceProvider, String file, String locale) {
    super(file, resourceProvider);
    String[] localeElements = locale.split("_");
    this.language = localeElements[0];
    this.region = localeElements[1];
  }

  /**
   * Returns the formatted given key according to the format specified on the configuration properties file
   *
   * @param configurationAttributeKey
   * @return an Optional with the formatted value of the given key or {@link Optional#empty()} otherwise.
   */
  @Override
  public Optional<ConfigurationProperty> provide(String configurationAttributeKey) {
    if (configurationAttributeKey.startsWith(LOCALE_PREFIX)) {
      String effectiveKey = configurationAttributeKey.substring(LOCALE_PREFIX.length());
      ConfigurationProperty property = super.provide("language.pattern").get();
      return of(new ConfigurationProperty() {

        @Override
        public Object getSource() {
          return this;
        }

        @Override
        public String getValue() {
          NumberFormat nf = NumberFormat.getInstance(new Locale(language, region));
          DecimalFormat formatter = (DecimalFormat) nf;
          formatter.applyPattern(property.getValue());
          return formatter.format(Double.parseDouble(effectiveKey));
        }

        @Override
        public String getKey() {
          return effectiveKey;
        }
      });
    } else {
      return empty();
    }
  }

  @Override
  protected String createValue(String key, String value) {
    return format("%s", value);
  }
}
