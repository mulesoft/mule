package org.mule.runtime.module.extension.internal.runtime.source.legacy;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.internal.util.message.SdkResultAdapter;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.sdk.api.runtime.source.SourceCallback;

public class SdkToLegacySourceCallbackAdapter<T, A> implements org.mule.runtime.extension.api.runtime.source.SourceCallback<T, A> {

  private final SourceCallback<T, A> sourceCallback;

  public SdkToLegacySourceCallbackAdapter(SourceCallback<T, A> sourceCallback) {
    this.sourceCallback = sourceCallback;
  }

  @Override
  public void handle(Result<T, A> result) {
    sourceCallback.handle(SdkResultAdapter.from(result));
  }

  @Override
  public void handle(Result<T, A> result, SourceCallbackContext context) {
    sourceCallback.handle(SdkResultAdapter.from(result), new LegacyToSdkSourceCallBackContextAdapter(context));
  }

  @Override
  public void onConnectionException(ConnectionException e) {
    sourceCallback.onConnectionException(e);
  }

  @Override
  public SourceCallbackContext createContext() {
    return new SdkToLegacySourceCallbackContextAdapter(sourceCallback.createContext());
  }
}
