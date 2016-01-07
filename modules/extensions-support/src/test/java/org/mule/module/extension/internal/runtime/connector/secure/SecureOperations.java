package org.mule.module.extension.internal.runtime.connector.secure;

import org.mule.extension.annotation.api.Operation;
import org.mule.extension.annotation.api.param.display.Password;

public class SecureOperations
{

    @Operation
    public String dummyOperation(@Password String secureParam)
    {
        return secureParam;
    }
}
