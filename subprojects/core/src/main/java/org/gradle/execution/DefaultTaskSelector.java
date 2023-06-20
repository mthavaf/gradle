/*
 * Copyright 2010 the original author or authors.
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
package org.gradle.execution;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.project.ProjectState;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.problems.Problems;
import org.gradle.api.problems.interfaces.ProblemGroup;
import org.gradle.api.specs.Spec;
import org.gradle.util.internal.NameMatcher;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DefaultTaskSelector implements TaskSelector {
    private static final Logger LOGGER = Logging.getLogger(DefaultTaskSelector.class);

    private final TaskNameResolver taskNameResolver;
    private final ProjectConfigurer configurer;

    public DefaultTaskSelector(TaskNameResolver taskNameResolver, ProjectConfigurer configurer) {
        this.taskNameResolver = taskNameResolver;
        this.configurer = configurer;
    }

    @Override
    public Spec<Task> getFilter(SelectionContext context, ProjectState project, String taskName, boolean includeSubprojects) {
        if (includeSubprojects) {
            // Try to delay configuring all the subprojects
            configurer.configure(project.getMutableModel());
            if (taskNameResolver.tryFindUnqualifiedTaskCheaply(taskName, project.getMutableModel())) {
                // An exact match in the target project - can just filter tasks by path to avoid configuring subprojects at this point
                return new TaskPathSpec(project.getMutableModel(), taskName);
            }
        }

        final Set<Task> selectedTasks = getSelection(context, project, taskName, includeSubprojects).getTasks();
        return element -> !selectedTasks.contains(element);
    }

    @Override
    public TaskSelection getSelection(SelectionContext context, ProjectState targetProject, String taskName, boolean includeSubprojects) {
        if (!includeSubprojects) {
            configurer.configure(targetProject.getMutableModel());
        } else {
            configurer.configureHierarchy(targetProject.getMutableModel());
        }

        TaskSelectionResult tasks = taskNameResolver.selectWithName(taskName, targetProject.getMutableModel(), includeSubprojects);
        if (tasks != null) {
            LOGGER.info("Task name matched '{}'", taskName);
            return new TaskSelection(targetProject.getProjectPath().getPath(), taskName, tasks);
        } else {
            Map<String, TaskSelectionResult> tasksByName = taskNameResolver.selectAll(targetProject.getMutableModel(), includeSubprojects);
            NameMatcher matcher = new NameMatcher();
            String actualName = matcher.find(taskName, tasksByName.keySet());

            if (actualName != null) {
                LOGGER.info("Abbreviated task name '{}' matched '{}'", taskName, actualName);
                return new TaskSelection(targetProject.getProjectPath().getPath(), taskName, tasksByName.get(actualName));
            }

            String searchContext;
            if (includeSubprojects && !targetProject.getChildProjects().isEmpty()) {
                searchContext = targetProject.getDisplayName() + " and its subprojects";
            } else {
                searchContext = targetProject.getDisplayName().getDisplayName();
            }

            if (context.getOriginalPath().getPath().equals(taskName)) {
                throw new TaskSelectionException(matcher.formatErrorMessage("Task", searchContext));
            } else {
                String message = String.format("Cannot locate %s that match '%s' as %s", context.getType(), context.getOriginalPath(),
                    matcher.formatErrorMessage("task", searchContext));
//                throw Problems.throwing(Problems.createError(ProblemGroup.GENERIC, message)
//                    .location(Objects.requireNonNull(context.getOriginalPath().getName()), -1), new TaskSelectionException(message));

                throw Problems.createError(ProblemGroup.GENERIC, message, null)
                    .location(Objects.requireNonNull(context.getOriginalPath().getName()), -1)
                    .undocumented()
                    .cause(new TaskSelectionException(message))
                    .throwIt();
            }
        }
    }

    private static class TaskPathSpec implements Spec<Task> {
        private final ProjectInternal targetProject;
        private final String taskName;

        public TaskPathSpec(ProjectInternal targetProject, String taskName) {
            this.targetProject = targetProject;
            this.taskName = taskName;
        }

        @Override
        public boolean isSatisfiedBy(Task element) {
            if (!element.getName().equals(taskName)) {
                return true;
            }
            for (Project current = element.getProject(); current != null; current = current.getParent()) {
                if (current.equals(targetProject)) {
                    return false;
                }
            }
            return true;
        }
    }
}
