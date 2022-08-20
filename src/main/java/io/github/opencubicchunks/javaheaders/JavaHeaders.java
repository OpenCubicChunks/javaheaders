package io.github.opencubicchunks.javaheaders;

import io.github.opencubicchunks.javaheaders.LinkHeadersTransform.Parameters;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.attributes.Attribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaHeaders implements Plugin<Project> {
    public static final Logger LOGGER = LoggerFactory.getLogger("");

    @Override
    public void apply(Project project) {
        DependencyHandler dependencies = project.getDependencies();
        Attribute<String> artifactType = Attribute.of("artifactType", String.class);
        Attribute<Boolean> javaHeadersLinked = Attribute.of("javaHeadersLinked", Boolean.class);

        ConfigurationContainer configurations = project.getConfigurations();
        configurations.all((config) -> {
            if (config.isCanBeResolved()) {
                config.getAttributes().attribute(javaHeadersLinked, true);
            }
        });

        dependencies.getAttributesSchema().attribute(javaHeadersLinked);
        // set all jar dependencies to default to javaHeadersLinked false
        dependencies.getArtifactTypes().getByName("jar").getAttributes().attribute(javaHeadersLinked, false);

        dependencies.registerTransform(LinkHeadersTransform.class, transformSpec -> {
            transformSpec.getFrom().attribute(javaHeadersLinked, false).attribute(artifactType, "jar");
            transformSpec.getTo().attribute(javaHeadersLinked, true).attribute(artifactType, "jar");

            Parameters parameters = transformSpec.getParameters();
            LinkHeadersExtension extension = new LinkHeadersExtension(parameters);
            project.getExtensions().add("javaHeaders", extension);
        });
    }
}
