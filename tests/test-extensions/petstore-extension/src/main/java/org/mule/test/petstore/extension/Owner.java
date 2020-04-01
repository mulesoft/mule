package org.mule.test.petstore.extension;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.io.InputStream;

public class Owner {

    @Parameter
    @Optional
    public InputStream ownerName;

    @Parameter
    @Optional
    public TypedValue<InputStream> ownerSignature;

    public InputStream getOwnerName() {
        return ownerName;
    }

    public TypedValue<InputStream> getOwnerSignature() {
        return ownerSignature;
    }

}
