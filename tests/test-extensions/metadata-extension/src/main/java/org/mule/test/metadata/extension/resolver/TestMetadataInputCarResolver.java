package org.mule.test.metadata.extension.resolver;

import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.getCarMetadata;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;

public class TestMetadataInputCarResolver implements InputTypeResolver<String> {

  public static String TEST_INPUT_CAR_RESOLVER = "testInputCarResolver";

  @Override
  public String getCategoryName() {
    return "TestResolvers";
  }

  @Override
  public MetadataType getInputMetadata(MetadataContext context, String key) throws MetadataResolvingException, ConnectionException {
    return getCarMetadata();
  }

  @Override
  public String getResolverName() {
    return TEST_INPUT_CAR_RESOLVER;
  }
}
