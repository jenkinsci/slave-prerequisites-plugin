/*
 * Copyright (C) 2012 CloudBees Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package com.cloudbees.plugins;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.*;
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;
import hudson.tasks.BatchFile;
import hudson.tasks.CommandInterpreter;
import hudson.tasks.Shell;
import hudson.util.ListBoxModel;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.List;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static hudson.model.TaskListener.NULL;

/**
 * @author: <a hef="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class JobPrerequisites extends JobProperty<AbstractProject<?, ?>> implements Action {

    private final String script;
    private final String interpreter;

    public static final String SHELL_SCRIPT = "shell script";
    public static final String WINDOWS = "windows batch command";

    @DataBoundConstructor
    public JobPrerequisites(String script, String interpreter) {
        this.script = script;
        this.interpreter = interpreter;
    }

    public String getScript() {
        return script;
    }

    /**
     * @return true if all prerequisites a met on the target Node
     */
    public CauseOfBlockage check(Node node, Queue.BuildableItem item)
            throws IOException, InterruptedException {
        CommandInterpreter shell = getCommandInterpreter(this.script);
        FilePath root = node.getRootPath();
        if (root == null) return new CauseOfBlockage.BecauseNodeIsOffline(node); //offline ?

        HashMap<String, String> envs = new HashMap<String, String>();
        envs.put("PARAMS", item.getParams());

        FilePath scriptFile = shell.createScriptFile(root);
        shell.buildCommandLine(scriptFile);
        int r = node.createLauncher(NULL).launch().cmds(shell.buildCommandLine(scriptFile))
                .envs(envs).stdout(NULL).pwd(root).start().joinWithTimeout(60, TimeUnit.SECONDS, NULL);
        scriptFile.delete();
        return r == 0 ? null : new BecausePrerequisitesArentMet(node);
    }

    private CommandInterpreter getCommandInterpreter(String script) {
        if (WINDOWS.equals(interpreter)) {
            return new BatchFile(script);
        }
        return new Shell(script);
    }

    @Extension
    public final static class DescriptorImpl extends JobPropertyDescriptor {

        @Override
        public String getDisplayName() {
            return "Check prerequisites before job can build on a slave node";
        }

        @Override
        public boolean isApplicable(Class<? extends Job> jobType) {
            return AbstractProject.class.isAssignableFrom(jobType);
        }

        @Override
        public JobProperty<?> newInstance(StaplerRequest req, JSONObject formData) throws FormException {
            if (formData.isNullObject()) {
                return null;
            }
            JSONObject prerequisites = formData.getJSONObject("prerequisites");
            if (prerequisites.isNullObject()) {
                return null;
            }
            return req.bindJSON(JobPrerequisites.class,prerequisites);
        }

        public ListBoxModel doFillInterpreterItems() {
            return new ListBoxModel()
                    .add(SHELL_SCRIPT)
                    .add(WINDOWS);
        }

    }

    // fake implementations for Action, required to contribute the job configuration UI

    public String getDisplayName() {
        return null;
    }

    public String getIconFileName() {
        return null;
    }

    public String getUrlName() {
        return null;
    }

}
