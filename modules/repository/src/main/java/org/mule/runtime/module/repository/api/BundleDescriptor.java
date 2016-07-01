/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.repository.api;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;

/**
 * Descriptor to identify a bundle
 *
 * @since 4.0
 */
public class BundleDescriptor
{

    private String groupId;
    private String artifactId;
    private String version;
    private String type;

    private BundleDescriptor()
    {
    }

    public String getGroupId()
    {
        return this.groupId;
    }

    public String getArtifactId()
    {
        return this.artifactId;
    }

    public String getVersion()
    {
        return this.version;
    }

    public String getType()
    {
        return type;
    }

    public String toString()
    {
        return format("BundleDescriptor { %s:%s:%s:$s }", groupId, artifactId, version, type);
    }

    /**
     * Builder for creating a {@code BundleDescriptor}
     */
    public static class Builder
    {

        private static final String REQUIRED_FIELD_NOT_FOUND_TEMPLATE = "bundle cannot be created with null %s";

        private BundleDescriptor bundleDescriptor = new BundleDescriptor();

        /**
         * @param groupId the group id of the bundle
         * @return the builder
         */
        public Builder setGroupId(String groupId)
        {
            bundleDescriptor.groupId = groupId;
            return this;
        }

        /**
         * @param artifactId the artifactId id of the bundle
         * @return the builder
         */
        public Builder setArtifactId(String artifactId)
        {
            bundleDescriptor.artifactId = artifactId;
            return this;
        }

        /**
         * This is the version of the bundle.
         *
         * @param version the version of the bundle
         * @return the builder
         */
        public Builder setVersion(String version)
        {
            bundleDescriptor.version = version;
            return this;
        }

        /**
         * Sets the extension type of the bundle.
         *
         * @param type the type id of the bundle
         * @return the builder
         */
        public Builder setType(String type)
        {
            bundleDescriptor.type = type;
            return this;
        }

        /**
         * @return builds the bundle descriptor.
         */
        public BundleDescriptor build()
        {
            checkState(bundleDescriptor.groupId != null, getNullFieldMessage("group id"));
            checkState(bundleDescriptor.artifactId != null, getNullFieldMessage("artifact id"));
            checkState(bundleDescriptor.version != null, getNullFieldMessage("version"));
            checkState(bundleDescriptor.type != null, getNullFieldMessage("type"));
            return this.bundleDescriptor;
        }

        private String getNullFieldMessage(String field)
        {
            return format(REQUIRED_FIELD_NOT_FOUND_TEMPLATE, field);
        }

    }


}
