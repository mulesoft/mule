/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth;

import static java.util.Arrays.asList;
import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.metadata.TypeResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Query;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.runtime.extension.api.connectivity.oauth.AccessTokenExpiredException;
import org.mule.runtime.extension.api.connectivity.oauth.AuthorizationCodeState;
import org.mule.runtime.extension.api.connectivity.oauth.OAuthState;
import org.mule.runtime.extension.api.dsql.QueryTranslator;
import org.mule.runtime.extension.api.error.MuleErrors;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;
import org.mule.sdk.api.annotation.data.sample.SampleData;
import org.mule.test.oauth.metadata.OAuthMetadataResolver;
import org.mule.test.oauth.metadata.RefreshedOAuthMetadataResolver;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

public class TestOAuthOperations {

  private static int executedCounter = 0;

  public TestOAuthConnection getConnection(@Connection TestOAuthConnection connection) {
    return connection;
  }

  public void tokenExpired(@Connection TestOAuthConnection connection) {
    final OAuthState state = connection.getState().getState();
    if (state != null && !state.getAccessToken().endsWith("refreshed")) {
      if (state instanceof AuthorizationCodeState) {
        throw new AccessTokenExpiredException(((AuthorizationCodeState) state).getResourceOwnerId());
      } else {
        throw new AccessTokenExpiredException();
      }
    }
  }

  @MediaType(TEXT_PLAIN)
  public void tokenExpiredAsync(@Connection TestOAuthConnection connection,
                                CompletionCallback<String, String> completionCallback) {
    final OAuthState state = connection.getState().getState();
    if (state != null && !state.getAccessToken().endsWith("refreshed")) {
      if (state instanceof AuthorizationCodeState) {
        completionCallback.error(new AccessTokenExpiredException(((AuthorizationCodeState) state).getResourceOwnerId()));
      } else {
        completionCallback.error(new AccessTokenExpiredException());
      }
    } else {
      completionCallback.success(Result.<String, String>builder().output("Success!").attributes("Success!").build());
    }
  }

  public PagingProvider<TestOAuthConnection, String> pagedOperation(@Optional(defaultValue = "0") Integer failAt) {
    return new PagingProvider<TestOAuthConnection, String>() {

      private int count = 1;
      private boolean done = false;

      @Override
      public List<String> getPage(TestOAuthConnection connection) {
        if (done) {
          return null;
        }

        final OAuthState state = connection.getState().getState();

        if (count >= failAt) {
          if (!state.getAccessToken().endsWith("refreshed")) {
            if (state instanceof AuthorizationCodeState) {
              throw new ModuleException(MuleErrors.CONNECTIVITY,
                                        new AccessTokenExpiredException(((AuthorizationCodeState) state).getResourceOwnerId()));
            } else {
              throw new ModuleException(MuleErrors.CONNECTIVITY, new AccessTokenExpiredException());
            }
          } else {
            done = true;
          }
        }

        return asList("item " + count++);
      }

      @Override
      public java.util.Optional<Integer> getTotalResults(TestOAuthConnection connection) {
        return java.util.Optional.empty();
      }

      @Override
      public void close(TestOAuthConnection connection) throws MuleException {

      }
    };
  }

  public TestOAuthConnection getFlackyConnection(@Connection TestOAuthConnection connection) {
    if (executedCounter++ % 2 == 0) {
      throw new AccessTokenExpiredException();
    }
    return connection;
  }

  @MediaType(ANY)
  public InputStream getStream(String content) {
    return new ByteArrayInputStream(content.getBytes());
  }

  @MediaType(ANY)
  public String getStreamContentWithFlackyConnection(@Connection TestOAuthConnection connection, InputStream content) {
    String result = IOUtils.toString(content);
    if (executedCounter++ % 2 == 0) {
      throw new AccessTokenExpiredException();
    }

    return result;
  }

  @OutputResolver(output = RefreshedOAuthMetadataResolver.class)
  @MediaType(TEXT_PLAIN)
  public String metadataOperation(@MetadataKeyId(RefreshedOAuthMetadataResolver.class) String metadataKey,
                                  @TypeResolver(RefreshedOAuthMetadataResolver.class) Object inputParameter,
                                  @Connection TestOAuthConnection connection) {
    return "Operation Result";
  }

  @OutputResolver(attributes = RefreshedOAuthMetadataResolver.class, output = OAuthMetadataResolver.class)
  @MediaType(TEXT_PLAIN)
  public Result<String, String> anotherMetadataOperation(@MetadataKeyId(RefreshedOAuthMetadataResolver.class) String metadataKey,
                                                         @TypeResolver(RefreshedOAuthMetadataResolver.class) Object inputParameter,
                                                         @org.mule.sdk.api.annotation.param.Connection TestOAuthConnection connection) {
    return Result.<String, String>builder().output("Operation Result").attributes("Operation Attributes").build();
  }

  @Query(translator = QueryTranslator.class,
      entityResolver = RefreshedOAuthMetadataResolver.class,
      nativeOutputResolver = RefreshedOAuthMetadataResolver.class)
  @MediaType(TEXT_PLAIN)
  public String entitiesMetadataOperation(@MetadataKeyId String key, @Connection TestOAuthConnection connection) {
    return "Operation Result";
  }

  @MediaType(TEXT_PLAIN)
  public String valuesOperation(@OfValues(OAuthValuesProvider.class) String parameter,
                                @Connection TestOAuthConnection connection) {
    return "Operation Result";
  }

  @MediaType(TEXT_PLAIN)
  @SampleData(RefreshedOAuthSampleDataProvider.class)
  public Result<String, String> sampleDataOperation(@Connection TestOAuthConnection connection) {
    return Result.<String, String>builder().output("Operation Result").attributes("Operation Attributes").build();
  }
}
