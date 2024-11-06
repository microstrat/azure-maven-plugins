/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud.config;

import com.microsoft.azure.maven.springcloud.AbstractMojoBase;
import com.microsoft.azure.maven.utils.MavenArtifactUtils;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudClusterConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class ConfigurationParser {
    public SpringCloudAppConfig parse(AbstractMojoBase springMojo) {
        final AppDeploymentMavenConfig rawConfig = springMojo.getDeployment();
        final SpringCloudDeploymentConfig config = ConfigurationParser.toDeploymentConfig(rawConfig, springMojo);
        final SpringCloudClusterConfig clusterConfig = ConfigurationParser.toClusterConfig(springMojo);
        return SpringCloudAppConfig.builder()
            .appName(springMojo.getAppName())
            .cluster(clusterConfig)
            .deployment(config)
            .isPublic(springMojo.getIsPublic())
            .build();
    }

    private static SpringCloudClusterConfig toClusterConfig(@Nonnull final AbstractMojoBase springMojo) {
        return SpringCloudClusterConfig.builder()
            .clusterName(springMojo.getClusterName())
            .resourceGroup(springMojo.getResourceGroup())
            .region(springMojo.getRegion())
            .subscriptionId(springMojo.getSubscriptionId())
            .sku(springMojo.getSku())
//            .environment(springMojo.getEnvironment())
//            .environmentResourceGroup(StringUtils.firstNonBlank(springMojo.getEnvironmentResourceGroup(), springMojo.getResourceGroup()))
            .build();
    }

    @SneakyThrows
    private static SpringCloudDeploymentConfig toDeploymentConfig(AppDeploymentMavenConfig rawConfig, AbstractMojoBase mojo) {
        final List<File> artifacts = new ArrayList<>();
        Optional.ofNullable(rawConfig.getResources()).ifPresent(resources -> resources.forEach(resource -> {
            try {
                artifacts.addAll(MavenArtifactUtils.getArtifacts(resource));
            } catch (IllegalStateException e) {
                AzureMessager.getMessager().warning(String.format("'%s' doesn't exist or isn't a directory", resource.getDirectory()));
            }
        }));
        if (artifacts.isEmpty()) {
            artifacts.addAll(MavenArtifactUtils.getArtifactFiles(mojo.getProject()));
        }
        final File artifact = MavenArtifactUtils.getExecutableJarFiles(artifacts);
        return SpringCloudDeploymentConfig.builder()
            .cpu(rawConfig.getCpu())
            .deploymentName(Utils.emptyToNull(rawConfig.getDeploymentName()))
            .artifact(artifact != null ? IArtifact.fromFile(artifact) : null)
            .enablePersistentStorage(rawConfig.isEnablePersistentStorage())
            .environment(retrieveEnvironment(rawConfig))
            .capacity(rawConfig.getInstanceCount())
            .jvmOptions(Utils.emptyToNull(rawConfig.getJvmOptions()))
            .memoryInGB(rawConfig.getMemoryInGB())
            .runtimeVersion(Utils.emptyToNull(StringUtils.firstNonEmpty(rawConfig.getRuntimeVersion(), mojo.getRuntimeVersion())))
            .build();
    }

    private static @Nullable Map<String, String> retrieveEnvironment(final AppDeploymentMavenConfig rawConfig) {
        final Map<String, String> environment = new HashMap<>();
        if (hasEnvironmentPropertiesFiles(rawConfig)) {
            //noinspection DataFlowIssue - handled by hasEnvironmentPropertiesFiles
            rawConfig.getEnvironmentPropFiles().forEach(file -> addPropertiesFromFile(file, environment));
        }

        if (rawConfig.getEnvironment() != null) {
            environment.putAll(rawConfig.getEnvironment());
        }

        return Utils.emptyToNull(environment);
    }

    private static boolean hasEnvironmentPropertiesFiles(final AppDeploymentMavenConfig rawConfig) {
        return rawConfig.getEnvironmentPropFiles() != null && !rawConfig.getEnvironmentPropFiles().isEmpty();
    }

    private static void addPropertiesFromFile(final File file, final Map<String, String> environment) {
        readPropertiesFromFile(file).forEach((key, value) -> environment.put(key.toString(), value.toString()));
    }

    private static @NotNull Properties readPropertiesFromFile(final File file) {
        Properties props = new Properties();
        try (InputStream inStream = file.toURI().toURL().openStream()) {
            props.load(inStream);
        } catch (Exception e) {
            AzureMessager.getMessager().warning(String.format("'%s' doesn't exist or isn't a directory", file.getAbsolutePath()));
        }
        return props;
    }

    public static ConfigurationParser getInstance() {
        return Holder.parser;
    }

    private static class Holder {
        private static final ConfigurationParser parser = new ConfigurationParser();
    }
}
