/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.build;

import org.gradle.api.internal.GradleInternal;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.project.ProjectState;
import org.gradle.internal.UncheckedException;
import org.gradle.internal.build.BuildLifecycleController;
import org.gradle.internal.build.BuildState;
import org.gradle.internal.build.BuildToolingModelController;
import org.gradle.internal.operations.BuildOperationExecutor;
import org.gradle.internal.operations.RunnableBuildOperation;
import org.gradle.internal.resources.ProjectLeaseRegistry;
import org.gradle.tooling.provider.model.UnknownModelException;
import org.gradle.tooling.provider.model.internal.ToolingModelBuilderLookup;

import java.util.Collection;

class DefaultBuildToolingModelController implements BuildToolingModelController {
    private final BuildLifecycleController buildController;
    private final ProjectLeaseRegistry projectLeaseRegistry;
    private final BuildOperationExecutor buildOperationExecutor;
    private final Object treeMutableStateLock;
    private final boolean parallelActions;

    public DefaultBuildToolingModelController(
        BuildLifecycleController buildController,
        ProjectLeaseRegistry projectLeaseRegistry,
        BuildOperationExecutor buildOperationExecutor,
        Object treeMutableStateLock,
        boolean parallelActions
    ) {
        this.buildController = buildController;
        this.projectLeaseRegistry = projectLeaseRegistry;
        this.buildOperationExecutor = buildOperationExecutor;
        this.treeMutableStateLock = treeMutableStateLock;
        this.parallelActions = parallelActions;
    }

    @Override
    public GradleInternal getConfiguredModel() {
        return buildController.getConfiguredBuild();
    }

    @Override
    public ToolingModelBuilderLookup.Builder locateBuilderForDefaultTarget(String modelName, boolean param) {
        synchronized (treeMutableStateLock) {
            // Look for a build scoped builder
            ToolingModelBuilderLookup lookup = buildController.getGradle().getServices().get(ToolingModelBuilderLookup.class);
            ToolingModelBuilderLookup.Builder builder = lookup.maybeLocateForBuildScope(modelName, param, buildController.getGradle().getOwner());
            if (builder != null) {
                return builder;
            }

            // Locate a project scoped model using default project
            return locateBuilderForTarget(buildController.getGradle().getOwner(), modelName, param);
        }
    }

    @Override
    public ToolingModelBuilderLookup.Builder locateBuilderForTarget(BuildState target, String modelName, boolean param) throws UnknownModelException {
        synchronized (treeMutableStateLock) {
            // Force configuration of the build and locate builder for default project
            buildController.getGradle().getOwner().ensureProjectsConfigured();
            target.ensureProjectsConfigured();
        }
        ProjectInternal targetProject = target.getMutableModel().getDefaultProject();
        return doLocate(targetProject.getOwner(), modelName, param);
    }

    @Override
    public ToolingModelBuilderLookup.Builder locateBuilderForTarget(ProjectState target, String modelName, boolean param) throws UnknownModelException {
        synchronized (treeMutableStateLock) {
            // Force configuration of the containing build and then locate the builder for target project
            buildController.getGradle().getOwner().ensureProjectsConfigured();
            target.getOwner().ensureProjectsConfigured();
        }
        return doLocate(target, modelName, param);
    }

    private ToolingModelBuilderLookup.Builder doLocate(ProjectState target, String modelName, boolean param) {
        // Force configuration of the target project to ensure all builders have been registered
        target.ensureConfigured();
        ToolingModelBuilderLookup lookup = target.getMutableModel().getServices().get(ToolingModelBuilderLookup.class);
        return lookup.locateForClientOperation(modelName, param, target);
    }

    @Override
    public boolean queryModelActionsRunInParallel() {
        return projectLeaseRegistry.getAllowsParallelExecution() && parallelActions;
    }

    @Override
    public void runQueryModelActions(Collection<? extends RunnableBuildOperation> actions) {
        if (queryModelActionsRunInParallel()) {
            buildOperationExecutor.runAllWithAccessToProjectState(buildOperationQueue -> {
                for (RunnableBuildOperation action : actions) {
                    buildOperationQueue.add(action);
                }
            });
        } else {
            for (RunnableBuildOperation action : actions) {
                try {
                    action.run(null);
                } catch (Exception e) {
                    throw UncheckedException.throwAsUncheckedException(e);
                }
            }
        }
    }
}
