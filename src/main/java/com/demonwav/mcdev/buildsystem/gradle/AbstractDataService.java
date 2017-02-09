/*
 * Minecraft Dev for IntelliJ
 *
 * https://minecraftdev.org
 *
 * Copyright (c) 2017 minecraft-dev
 *
 * MIT License
 */

package com.demonwav.mcdev.buildsystem.gradle;

import com.demonwav.mcdev.platform.AbstractModuleType;
import com.demonwav.mcdev.platform.MinecraftModule;
import com.demonwav.mcdev.platform.MinecraftModuleType;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.LibraryDependencyData;
import com.intellij.openapi.externalSystem.model.project.LibraryPathType;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Order(ExternalSystemConstants.UNORDERED)
public abstract class AbstractDataService extends AbstractProjectDataService<LibraryDependencyData, Module> {

    @NotNull
    private final AbstractModuleType<?> type;

    public AbstractDataService(@NotNull final AbstractModuleType type) {
        this.type = type;
    }

    @NotNull
    @Override
    public Key<LibraryDependencyData> getTargetDataKey() {
        return ProjectKeys.LIBRARY_DEPENDENCY;
    }

    @Override
    public void importData(@NotNull Collection<DataNode<LibraryDependencyData>> toImport,
                           @Nullable ProjectData projectData,
                           @NotNull Project project,
                           @NotNull IdeModifiableModelsProvider modelsProvider) {

        if (projectData == null || !projectData.getOwner().equals(GradleConstants.SYSTEM_ID)) {
            return;
        }

        Set<Module> goodModules = Sets.newHashSet();
        Set<Module> allModules = Sets.newHashSet();

        for (DataNode<LibraryDependencyData> node : toImport) {
            final Module module = modelsProvider.findIdeModule(node.getData().getOwnerModule());
            allModules.add(module);
            if (node.getData().getExternalName().toLowerCase().startsWith(type.getGroupId() + ":" + type.getArtifactId())) {
                goodModules.add(module);
            }
        }

        setupModules(goodModules, allModules, modelsProvider, type);
    }

    public static void setupModules(@NotNull Set<Module> goodModules,
                                    @NotNull Set<Module> allModules,
                                    @NotNull IdeModifiableModelsProvider modelsProvider,
                                    @NotNull AbstractModuleType<?> type) {

        // So the way the Gradle plugin sets it up is with 3 modules. There's the parent module, which the Gradle
        // dependencies don't apply to, then submodules under it, normally main and test, which the Gradle dependencies
        // do apply to. We're interested (when setting the module type) in the parent, which is what we do here. The
        // first module should be the parent, but we check to make sure anyways
        ApplicationManager.getApplication().runReadAction(() -> {
            Set<Module> checkedModules = new HashSet<>();
            Set<Module> badModules = new HashSet<>();
            checkedModules.addAll(goodModules);

            goodModules.stream()
                .filter(Objects::nonNull)
                .forEach(m -> findParent(m, modelsProvider, type, checkedModules, badModules));

            // Reset all other modules back to JavaModule && remove the type
            for (Module module : allModules) {
                if (module == null) {
                    continue;
                }
                if (!checkedModules.contains(module) || badModules.contains(module)) {
                    if (Strings.nullToEmpty(module.getOptionValue("type")).equals(type.getId())) {
                        module.setOption("type", JavaModuleType.getModuleType().getId());
                    }
                    MinecraftModuleType.removeOption(module, type.getId());
                }
            }
        });
    }

    protected void checkModule(@NotNull Collection<DataNode<LibraryDependencyData>> toImport,
                               @NotNull IdeModifiableModelsProvider modelsProvider,
                               @NotNull AbstractModuleType<?> type,
                               @NotNull String... texts) {

        ApplicationManager.getApplication().runReadAction(() -> {

            final Set<Module> goodModules = Sets.newHashSet();
            final Set<Module> allModules = Sets.newHashSet();

            for (DataNode<LibraryDependencyData> node : toImport) {
                final Set<String> paths = node.getData().getTarget().getPaths(LibraryPathType.BINARY);
                paths.addAll(node.getData().getTarget().getPaths(LibraryPathType.SOURCE));

                final Module module = modelsProvider.findIdeModule(node.getData().getOwnerModule());
                allModules.add(module);

                if (paths.stream().anyMatch(p -> {
                    for (String text : texts) {
                        if (p.contains(text)) {
                            return true;
                        }
                    }
                    return false;
                })) {
                    goodModules.add(module);
                }
            }

            setupModules(goodModules, allModules, modelsProvider, type);
        });
    }

    private static void findParent(@NotNull Module module,
                                   @NotNull IdeModifiableModelsProvider modelsProvider,
                                   @NotNull AbstractModuleType type,
                                   @NotNull Set<Module> checkedModules,
                                   @NotNull Set<Module> badModules) {
        String[] path = modelsProvider.getModifiableModuleModel().getModuleGroupPath(module);
        if (path == null) {
            // Always reset back to JavaModule
            module.setOption("type", JavaModuleType.getModuleType().getId());
            checkedModules.add(module);
            MinecraftModuleType.addOption(module, type.getId());
        } else {
            String parentName = path[path.length - 1];
            Module parentModule = modelsProvider.getModifiableModuleModel().findModuleByName(parentName);
            if (parentModule != null) {
                // Always reset back to JavaModule
                parentModule.setOption("type", JavaModuleType.getModuleType().getId());
                badModules.add(module);
                checkedModules.add(parentModule);
                MinecraftModuleType.addOption(parentModule, type.getId());
                MinecraftModule.getInstance(parentModule);
            } else {
                return;
            }
        }
        Optional.ofNullable(MinecraftModule.getInstance(module)).ifPresent(m -> {
            if (m.getBuildSystem() != null && m.getIdeaModule() != null) {
                m.getBuildSystem().reImport(m.getIdeaModule());
            }
        });
    }
}
