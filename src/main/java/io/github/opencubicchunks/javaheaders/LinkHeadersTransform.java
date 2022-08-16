package io.github.opencubicchunks.javaheaders;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

import static io.github.opencubicchunks.javaheaders.JavaHeaders.LOGGER;

public abstract class LinkHeadersTransform implements TransformAction<LinkHeadersTransform.Parameters> {
    interface Parameters extends TransformParameters {
        @Input String getAcceptedJars();
        void setAcceptedJars(String acceptedJars);

        @Input long getDebug();
        void setDebug(long value);

        @InputFile File getConfig();
        void setConfig(File config);
    }

    @PathSensitive(PathSensitivity.NAME_ONLY)
    @InputArtifact
    public abstract Provider<FileSystemLocation> getInputArtifact();

    @Override
    public void transform(TransformOutputs outputs) {
        Config config = parseConfig(getParameters().getConfig());
        String fileName = getInputArtifact().get().getAsFile().getName();

        Pattern acceptedJars = Pattern.compile(getParameters().getAcceptedJars());

        if (acceptedJars.matcher(fileName).matches()) {
            LOGGER.warn(String.format("found %s", getInputArtifact().get().getAsFile()));

            String fileNameNoExt = fileName.substring(0, fileName.lastIndexOf("."));
            String outputFileName = fileNameNoExt + "-headerReplacement.jar";
            JavaHeadersTransformer.transformCoreLibrary(config, getInputArtifact().get().getAsFile(), outputs.file(outputFileName));

            LOGGER.warn(String.format("transformed %s", outputFileName));
            return;
        } else {
            LOGGER.info(String.format("Rejected jar %s", fileName));
        }
        outputs.file(getInputArtifact());
    }

    private Config parseConfig(File configFile) {
        Gson gson = new Gson();

        try {
            String fileString = new String(Files.readAllBytes(configFile.toPath()));
            return gson.fromJson(fileString, Config.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}