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
import hudson.model.queue.CauseOfBlockage;
import hudson.tasks.Shell;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static hudson.model.TaskListener.NULL;

/**
 * @author: <a hef="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class JobPrerequisites extends JobProperty<AbstractProject<?, ?>> implements Action {

    private final String script;
    
    @DataBoundConstructor
    public JobPrerequisites(String script) {
        this.script = script;
    }

    public String getScript() {
        return script;
    }

    /**
     * @return true if all prerequisites a met on the target Node
     */
    public CauseOfBlockage check(Node node) throws IOException, InterruptedException {
        Shell shell = new Shell(this.script);
        FilePath root = node.getRootPath();
        if (root == null) return new CauseOfBlockage.BecauseNodeIsOffline(node); //offline ?

        FilePath scriptFile = shell.createScriptFile(root);
        shell.buildCommandLine(scriptFile);
        int r = node.createLauncher(NULL).launch().cmds(shell.buildCommandLine(scriptFile))
                .stdout(NULL).pwd(root).start().joinWithTimeout(60, TimeUnit.SECONDS, NULL);
        return r == 0 ? null : new BecausePrerequisitesArentMet(node);
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
        public JobProperty<?> newInstance(StaplerRequest req,
                                          JSONObject formData) throws FormException {
            if (formData.isNullObject()) {
                return null;
            }

            JSONObject prerequisites = formData.getJSONObject("prerequisites");

            if (prerequisites.isNullObject()) {
                return null;
            }

            return new JobPrerequisites(prerequisites.getString("script"));
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
