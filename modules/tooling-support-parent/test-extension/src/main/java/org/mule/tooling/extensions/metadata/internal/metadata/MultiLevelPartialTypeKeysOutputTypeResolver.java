package org.mule.tooling.extensions.metadata.internal.metadata;

import static java.util.Collections.emptySet;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.PartialTypeKeysResolver;
import org.mule.tooling.extensions.metadata.api.parameters.LocationKey;
import org.mule.tooling.extensions.metadata.api.source.StringAttributes;

import java.util.Set;

public class MultiLevelPartialTypeKeysOutputTypeResolver implements PartialTypeKeysResolver<LocationKey>, OutputTypeResolver<LocationKey>,
        InputTypeResolver<LocationKey>, AttributesTypeResolver<LocationKey> {
  // continents
  public static final String AMERICA = "AMERICA";
  public static final String EUROPE = "EUROPE";

  // countries
  public static final String FRANCE = "FRANCE";
  public static final String ARGENTINA = "ARGENTINA";
  public static final String USA = "USA";
  public static final String USA_DISPLAY_NAME = "United States";

  // cities
  public static final String BUENOS_AIRES = "BA";
  public static final String LA_PLATA = "LPLT";
  public static final String PARIS = "PRS";
  public static final String SAN_FRANCISCO = "SFO";

  @Override
  public String getCategoryName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public MetadataKey resolveChilds(MetadataContext context, LocationKey partial)
          throws MetadataResolvingException, ConnectionException {

    // This is incomplete but you get the idea
    if (AMERICA.equalsIgnoreCase(partial.getContinent())) {

      if (ARGENTINA.equalsIgnoreCase(partial.getCountry())) {
        return newKey(AMERICA).withDisplayName(AMERICA)
                .withChild(newKey(ARGENTINA)
                                   .withChild(newKey(BUENOS_AIRES))
                                   .withChild(newKey(LA_PLATA)))
                .build();
      }

      return buildAmericaKey();

    } else if (EUROPE.equalsIgnoreCase(partial.getContinent())) {
      return buildEuropeKey();

    } else {
      throw new MetadataResolvingException("Invalid Continent", FailureCode.INVALID_METADATA_KEY);
    }
  }

  public static MetadataKey buildEuropeKey() {
    return newKey(EUROPE).withDisplayName(EUROPE).withChild(newKey(FRANCE).withChild(newKey(PARIS))).build();
  }

  public static MetadataKey buildAmericaKey() {
    return newKey(AMERICA).withDisplayName(AMERICA)
            .withChild(newKey(ARGENTINA).withChild(newKey(BUENOS_AIRES)).withChild(newKey(LA_PLATA)))
            .withChild(newKey(USA).withDisplayName(USA_DISPLAY_NAME).withChild(newKey(SAN_FRANCISCO))).build();
  }

  @Override
  public MetadataType getOutputType(MetadataContext metadataContext, LocationKey locationKey) throws MetadataResolvingException, ConnectionException {
    return BaseTypeBuilder.create(JAVA).stringType().defaultValue(locationKey.toString()).build();
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext metadataContext, LocationKey locationKey) throws MetadataResolvingException, ConnectionException {
    return BaseTypeBuilder.create(JAVA).stringType().defaultValue(locationKey.toString()).build();
  }

  @Override
  public MetadataType getAttributesType(MetadataContext metadataContext, LocationKey locationKey) throws MetadataResolvingException, ConnectionException {
    ObjectTypeBuilder objectTypeBuilder = BaseTypeBuilder.create(JAVA).objectType().id(StringAttributes.class.getName());
    objectTypeBuilder.addField().key("value").value(BaseTypeBuilder.create(JAVA).stringType().defaultValue(locationKey.toString()).build()).build();
    return objectTypeBuilder.build();
  }

  @Override
  public Set<MetadataKey> getKeys(MetadataContext metadataContext) throws MetadataResolvingException, ConnectionException {
    return emptySet();
  }

  @Override
  public String getResolverName() {
    return this.getClass().getSimpleName();
  }

}
