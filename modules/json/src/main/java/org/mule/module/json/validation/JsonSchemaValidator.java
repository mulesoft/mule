/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.json.validation;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.util.Preconditions.checkArgument;
import static org.mule.util.Preconditions.checkState;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.json.DefaultJsonParser;
import org.mule.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jsonschema.core.load.Dereferencing;
import com.github.fge.jsonschema.core.load.configuration.LoadingConfiguration;
import com.github.fge.jsonschema.core.load.configuration.LoadingConfigurationBuilder;
import com.github.fge.jsonschema.core.load.uri.URITranslatorConfiguration;
import com.github.fge.jsonschema.core.load.uri.URITranslatorConfigurationBuilder;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Validates json payloads against json schemas compliant with drafts v3 and v4.
 * <p/>
 * Instances are immutable and thread-safe. Correct way of instantiating this class
 * is by invoking {@link #builder()} to obtain a {@link Builder}
 *
 * @since 3.6.0
 */
public class JsonSchemaValidator
{

    /**
     * An implementation of the builder design pattern to create
     * instances of {@link JsonSchemaValidator}.
     * This builder can be safely reused, returning a different
     * instance each time {@link #build()} is invoked.
     * It is mandatory to invoke {@link #setSchemaLocation(String)}
     * with a valid value before
     * attempting to {@link #build()} an instance
     *
     * @since 3.6.0
     */
    public static final class Builder
    {

        private static final String RESOURCE_PREFIX = "resource:/";
        private String schemaLocation;
        private JsonSchemaDereferencing dereferencing = JsonSchemaDereferencing.CANONICAL;
        private final Map<String, String> schemaRedirects = new HashMap<>();

        private Builder()
        {
        }

        /**
         * A location in which the json schema is present. It allows both local and external resources. For example, all of the following are valid:
         * <li>
         * <ul>schemas/schema.json</ul>
         * <ul>/schemas/schema.json</ul>
         * <ul>resource:/schemas/schema.json</ul>
         * <ul>http://mule.org/schemas/schema.json</ul>
         * </li>
         *
         * @param schemaLocation the location of the schema to validate against
         * @return this builder
         * @throws IllegalArgumentException if {@code schemaLocation} is blank or {@code null}
         */
        public Builder setSchemaLocation(String schemaLocation)
        {
            checkArgument(!isBlank(schemaLocation), "schemaLocation cannot be null or blank");
            this.schemaLocation = formatUri(schemaLocation);
            return this;
        }

        /**
         * Sets the dereferencing mode to be used. If not invoked, then
         * it defaults to {@link JsonSchemaDereferencing#CANONICAL}
         *
         * @param dereferencing a dereferencing mode
         * @return this builder
         * @throws IllegalArgumentException if {@code dereferencing} is {@code null}
         */
        public Builder setDereferencing(JsonSchemaDereferencing dereferencing)
        {
            checkArgument(dereferencing != null, "dereferencing cannot be null");
            this.dereferencing = dereferencing;
            return this;
        }

        /**
         * Allows to redirect any given URI in the Schema (or even the schema location itself)
         * to any other specific URI. The most common use case for this feature is to map external
         * namespace URIs without the need to a local resource
         *
         * @param from the location to redirect. Accepts the same formats as {@link #setSchemaLocation(String)}
         * @param to   the location to redirect to. Accepts the same formats as {@link #setSchemaLocation(String)}
         * @return this builder
         * @throws IllegalArgumentException if {@code from} or {@code to} are blank or {@code null}
         */
        public Builder addSchemaRedirect(String from, String to)
        {
            checkArgument(!isBlank(from), "from cannot be null or blank");
            checkArgument(!isBlank(to), "to cannot be null or blank");
            schemaRedirects.put(formatUri(from), formatUri(to));

            return this;
        }

        /**
         * Allows adding many redirects following the same rules as {@link #addSchemaRedirect(String, String)}
         *
         * @param redirects a {@link Map} with redirections
         * @return this builder
         * @throws IllegalArgumentException if {@code redirects} is {@code null}
         */
        public Builder addSchemaRedirects(Map<String, String> redirects)
        {
            for (Map.Entry<String, String> redirect : redirects.entrySet())
            {
                addSchemaRedirect(redirect.getKey(), redirect.getValue());
            }

            return this;
        }

        /**
         * Builds a new instance per the given configuration. This method can be
         * safely invoked many times, returning a different instance each.
         *
         * @return a {@link JsonSchemaValidator}
         * @throws IllegalStateException if {@link #setSchemaLocation(String)} was not invoked
         */
        public JsonSchemaValidator build()
        {

            final URITranslatorConfigurationBuilder translatorConfigurationBuilder = URITranslatorConfiguration.newBuilder();
            for (Map.Entry<String, String> redirect : schemaRedirects.entrySet())
            {
                String key = resolveLocationIfNecessary(redirect.getKey());
                String value = resolveLocationIfNecessary(redirect.getValue());

                translatorConfigurationBuilder.addSchemaRedirect(key, value);
            }

            final LoadingConfigurationBuilder loadingConfigurationBuilder = LoadingConfiguration.newBuilder()
                    .dereferencing(dereferencing == JsonSchemaDereferencing.CANONICAL
                                   ? Dereferencing.CANONICAL
                                   : Dereferencing.INLINE)
                    .setURITranslatorConfiguration(translatorConfigurationBuilder.freeze());

            LoadingConfiguration loadingConfiguration = loadingConfigurationBuilder.freeze();
            JsonSchemaFactory factory = JsonSchemaFactory.newBuilder()
                    .setLoadingConfiguration(loadingConfiguration)
                    .freeze();

            try
            {
                return new JsonSchemaValidator(loadSchema(factory, loadingConfiguration));
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(MessageFactory.createStaticMessage("Could not initialise JsonSchemaValidator"), e);
            }
        }

        private JsonSchema loadSchema(JsonSchemaFactory factory, LoadingConfiguration loadingConfiguration) throws Exception
        {
            checkState(schemaLocation != null, "schemaLocation has not been provided");
            String realLocation = resolveLocationIfNecessary(schemaLocation);
            return factory.getJsonSchema(realLocation);
        }

        private String resolveLocationIfNecessary(String path)
        {
            URI uri = URI.create(path);

            String scheme = uri.getScheme();
            if (scheme == null || "resource".equals(scheme))
            {
                return openSchema(uri.getPath()).toString();
            }
            return path;
        }

        private URL openSchema(String path)
        {
            URL url = Thread.currentThread().getContextClassLoader().getResource(path);
            if (url == null && path.startsWith("/"))
            {
                return openSchema(path.substring(1));
            }

            return url;
        }

        private String formatUri(String location)
        {
            URI uri = URI.create(location);

            if (uri.getScheme() == null)
            {
                if (location.charAt(0) == '/')
                {
                    location = location.substring(1);
                }

                location = RESOURCE_PREFIX + location;
            }

            return location;
        }
    }

    /**
     * Returns a new {@link Builder}
     *
     * @return a {@link Builder}
     */
    public static Builder builder()
    {
        return new Builder();
    }

    private final JsonSchema schema;

    private JsonSchemaValidator(JsonSchema schema)
    {
        this.schema = schema;
    }

    /**
     * Parses the {@code event}'s payload as a Json by the rules of
     * {@link DefaultJsonParser#asJsonNode(Object)}. Then it validates it
     * against the given schema.
     * <p/>
     * If the validation fails, a {@link JsonSchemaValidationException} is thrown.
     * <p/>
     * Notice that if the message payload is a {@link Reader} or {@link InputStream}
     * then it will be consumed in order to perform the validation. As a result,
     * the message payload will be changed to the {@link String} representation
     * of the json.
     *
     * @param event the current {@link MuleEvent}
     */
    public void validate(MuleEvent event) throws MuleException
    {
        Object input = event.getMessage().getPayload();
        ProcessingReport report;
        JsonNode jsonNode = null;

        try
        {
            jsonNode = new DefaultJsonParser(event.getMuleContext()).asJsonNode(input);

            if ((input instanceof Reader) || (input instanceof InputStream))
            {
                event.getMessage().setPayload(jsonNode.toString());
            }

            report = schema.validate(jsonNode);
        }
        catch (Exception e)
        {
            throw new JsonSchemaValidationException("Exception was found while trying to validate json schema",
                                                    jsonNode == null ? StringUtils.EMPTY : jsonNode.toString(),
                                                    e);
        }

        if (!report.isSuccess())
        {
            throw new JsonSchemaValidationException("Json content is not compliant with schema\n" + report.toString(), jsonNode.toString());
        }
    }
}
