package io.github.opencubicchunks.javaheaders;

import java.util.regex.Pattern;

import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;

public abstract class LinkHeadersTransform implements TransformAction<LinkHeadersTransform.Parameters> {
    interface Parameters extends TransformParameters {
        @Input String getAcceptedJars();
        void setAcceptedJars(String acceptedJars);

        @Input long getDebug();
        void setDebug(long value);
    }

    @PathSensitive(PathSensitivity.NAME_ONLY)
    @InputArtifact
    public abstract Provider<FileSystemLocation> getInputArtifact();

    @Override
    public void transform(TransformOutputs outputs) {
        String fileName = getInputArtifact().get().getAsFile().getName();

        Pattern acceptedJars = Pattern.compile(getParameters().getAcceptedJars());

        if (acceptedJars.matcher(fileName).matches()) {
            JavaHeaders.LOGGER.info(String.format("found %s\n", fileName));

            String fileNameNoExt = fileName.substring(0, fileName.lastIndexOf("."));
            String outputFileName = fileNameNoExt + "-headerReplacement.jar";
            JavaHeadersTransformer.transformCoreLibrary(getInputArtifact().get().getAsFile(), outputs.file(outputFileName));

            JavaHeaders.LOGGER.info(String.format("transformed %s\n", outputFileName));
            return;
        }
        outputs.file(getInputArtifact());
    }
}