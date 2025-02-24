/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud.config;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.maven.model.Resource;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AppDeploymentMavenConfig {
    private Double cpu;
    private Double memoryInGB;
    /**
     * {@code max replicas} for apps of standard consumption plan or {@code instance num} for apps of other plans.
     */
    private Integer instanceCount;
    private String deploymentName;
    private String jvmOptions;
    private String runtimeVersion;
    private Boolean enablePersistentStorage;
    @Nullable
    private Map<String, String> environment;

    @Nullable
    private List<File> environmentPropFiles;

    @Nullable
    private List<Resource> resources;

    public Boolean isEnablePersistentStorage() {
        return BooleanUtils.isTrue(enablePersistentStorage);
    }
}
