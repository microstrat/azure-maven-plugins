/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.keyvaults;

import com.azure.resourcemanager.keyvault.models.SkuName;
import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.resourcemanager.keyvault.models.Vault.DefinitionStages.WithAccessPolicy;
import com.azure.resourcemanager.keyvault.models.Vault.DefinitionStages.WithCreate;
import com.azure.resourcemanager.keyvault.models.Vaults;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupDraft;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

public class KeyVaultDraft extends KeyVault implements AzResource.Draft<KeyVault, Vault> {
    @Getter
    @Nullable
    private KeyVault origin;
    @Nullable
    @Setter
    private Config config;

    protected KeyVaultDraft(@Nonnull String name, @Nonnull String resourceGroupName, @Nonnull KeyVaultModule module) {
        super(name, resourceGroupName, module);
    }

    protected KeyVaultDraft(@Nonnull KeyVault origin) {
        super(origin);
        this.origin = origin;
    }

    @Override
    public void reset() {
        this.config = null;
    }

    @Nonnull
    @Override
    public Vault createResourceInAzure() {
        final Vaults client = Objects.requireNonNull(((KeyVaultModule) getModule()).getClient(), "could not get key vault clients");
        final Region region = Objects.requireNonNull(ensureConfig().getRegion(),
            "'region' is required to create key vault.");
        final ResourceGroup resourceGroup = Objects.requireNonNull(ensureConfig().getResourceGroup(),
            "'resourceGroup' is required to create key vault.");
        final SkuName sku = Objects.requireNonNull(ensureConfig().getSku(),
            "'sku' is required to create key vault.");
        final boolean useAzureRBAC = ensureConfig().isUseAzureRBAC();
        if (resourceGroup.isDraftForCreating()) {
            ((ResourceGroupDraft) resourceGroup).setRegion(region);
            ((ResourceGroupDraft) resourceGroup).createIfNotExist();
        }
        final WithAccessPolicy withAccessPolicy = client.define(getName())
            .withRegion(region.getName())
            .withExistingResourceGroup(resourceGroup.getResourceGroupName());
        final WithCreate withCreate;
        if (useAzureRBAC) {
            withCreate = withAccessPolicy.withRoleBasedAccessControl();
        } else {
            final Account account = Objects.requireNonNull(Azure.az(AzureAccount.class).getAccount(), "could not get current account");
            final String username = account.getUsername();
            withCreate = withAccessPolicy.defineAccessPolicy().forUser(username)
                .allowCertificateAllPermissions()
                .allowKeyAllPermissions()
                .allowSecretAllPermissions()
                .allowStorageAllPermissions().attach();
        }
        return withCreate.withSku(sku).create();
    }

    @Nonnull
    @Override
    public Vault updateResourceInAzure(@Nonnull Vault origin) {
        throw new AzureToolkitRuntimeException("not supported");
    }

    @Override
    public boolean isModified() {
        return Objects.nonNull(config);
    }

    private Config ensureConfig() {
        this.config = Optional.ofNullable(config).orElseGet(Config::new);
        return this.config;
    }

    @Data
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config {
        private Subscription subscription;
        private ResourceGroup resourceGroup;
        private String name;
        private Region region;
        private SkuName sku;
        private boolean useAzureRBAC;
    }
}
