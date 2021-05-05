/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.gradle;

import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.tasks.userinput.UserInputHandler;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.openrewrite.gradle.RewriteReflectiveFacade.Environment;
import org.openrewrite.gradle.RewriteReflectiveFacade.NamedStyles;
import org.openrewrite.gradle.RewriteReflectiveFacade.OptionDescriptor;
import org.openrewrite.gradle.RewriteReflectiveFacade.RecipeDescriptor;
import org.openrewrite.gradle.ui.RecipeDescriptorTreePrompter;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Set;

public class RewriteDiscoverTask extends AbstractRewriteTask {
    private static final Logger log = Logging.getLogger(RewriteDiscoverTask.class);
    private boolean interactive;

    @Option(description = "Whether to enter an interactive shell to explore available recipes.", option = "interactive")
    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    @Input
    public boolean isInteractive() {
        return this.interactive;
    }

    @Override
    protected Logger getLog() {
        return log;
    }

    @Inject
    public RewriteDiscoverTask(Configuration configuration, SourceSet sourceSet, RewriteExtension extension) {
        super(configuration, sourceSet, extension);
        setGroup("rewrite");
        setDescription("Lists all available recipes and their visitors within the " + sourceSet.getName() + " SourceSet");
    }

    @TaskAction
    public void run() {
        Environment env = environment();
        Collection<RecipeDescriptor> availableRecipeDescriptors = env.listRecipeDescriptors();

        if (interactive) {
            log.quiet("Entering interactive mode, Ctrl-C to exit...");
            UserInputHandler prompter = getServices().get(UserInputHandler.class);
            RecipeDescriptorTreePrompter treePrompter = new RecipeDescriptorTreePrompter(prompter);
            RecipeDescriptor rd = treePrompter.execute(availableRecipeDescriptors);
            writeRecipeDescriptor(rd, true, 0);
        } else {
            Set<String> activeRecipes = getActiveRecipes();
            Collection<NamedStyles> availableStyles = env.listStyles();
            Set<String> activeStyles = getActiveStyles();

            log.quiet("Available Recipes:");
            for (RecipeDescriptor recipe : availableRecipeDescriptors) {
                log.quiet("\tname: " + recipe.getName());
            }

            log.quiet("Available Styles:");
            for (NamedStyles style : availableStyles) {
                log.quiet("\tname: " + style.getName());
            }

            log.quiet("Active Styles:");
            for (String style : activeStyles) {
                log.quiet("\tname: " + style);
            }

            log.quiet("Active Recipes:");
            for (String activeRecipe : activeRecipes) {
                log.quiet("\tname: " + activeRecipe);
            }

            log.quiet("Found " + availableRecipeDescriptors.size() + " available recipes and " + availableStyles.size() + " available styles.");
            log.quiet("Configured with " + activeRecipes.size() + " active recipes and " + activeStyles.size() + " active styles.");
        }

    }

    private void writeRecipeDescriptor(RecipeDescriptor rd, boolean verbose, int indentLevel) {
        log.quiet(indent(indentLevel, "name: " + rd.getName()));
        if (verbose) {
            log.quiet(indent(indentLevel, "displayName: " + rd.getDisplayName()));
            log.quiet(indent(indentLevel, "description: " + rd.getDescription()));

            log.quiet(indent(indentLevel, "options: " + (rd.getOptions().isEmpty() ? "[]" : "")));
            for (OptionDescriptor od : rd.getOptions()) {
                log.quiet(indent(indentLevel + 1, od.getName() + ": " + od.getType() + (od.isRequired() ? "!" : "")));
                log.quiet(indent(indentLevel + 2, "displayName: " + od.getDisplayName()));
                log.quiet(indent(indentLevel + 2, "description: " + od.getDescription()));
                log.quiet(indent(indentLevel + 2, (od.getExample() == null ? "" : "example: " + od.getExample())));
            }
        }

        if (verbose) {
            log.quiet(indent(indentLevel, ""));
        }

    }

}
