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
import hudson.matrix.MatrixConfiguration;
import hudson.model.AbstractProject;
import hudson.model.Node;
import hudson.model.Queue;
import hudson.model.queue.CauseOfBlockage;
import hudson.model.queue.QueueTaskDispatcher;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Let job check if node matches it's prerequisites just before starting a build on it
 */
@Extension
public class JobPrerequisitesChecker extends QueueTaskDispatcher {

    private static final Logger LOGGER = Logger.getLogger(JobPrerequisitesChecker.class.getName());

    ExecutorService pool = Executors.newCachedThreadPool();

    Map<String, Future<CauseOfBlockage>> futures = new HashMap<String, Future<CauseOfBlockage>>();
    
    @Override
    public CauseOfBlockage canTake(final Node node, Queue.BuildableItem item) {

        final JobPrerequisites prerequisite = getPrerequisite(item);
        if (prerequisite == null) return null;

        String key = key(item, node);
        Future<CauseOfBlockage> status = futures.get(key);
        if (status == null) {
            futures.put(key, pool.submit(new Callable<CauseOfBlockage>() {
                public CauseOfBlockage call() throws Exception {
                    try {
                        return prerequisite.check(node);
                    } catch (Exception e) {
                        return CauseOfBlockage.fromMessage(Messages._JobPrerequisitesChecker_FailedToCheckJobProrequisites(e.getMessage()));
                    }
                }
            }));
            return CHECKING;
        }
        if (!status.isDone()) return CHECKING;
        try {
            CauseOfBlockage blockage = status.get();
            futures.remove(key);
            return blockage;
        } catch (Exception e) {
            return CauseOfBlockage.fromMessage(Messages._JobPrerequisitesChecker_FailedToCheckJobProrequisites(e.getMessage()));
        }
    }

    private final static CauseOfBlockage CHECKING = CauseOfBlockage.fromMessage(Messages._JobPrerequisitesChecker_CheckingJobPrerequisites());

    private String key(Queue.Item item, Node node) {
        return String.valueOf(item.id)+":"+node.getNodeName();
    }

    private JobPrerequisites getPrerequisite(Queue.BuildableItem item) {
        Queue.Task task = item.task;
        if (task instanceof AbstractProject) {
            AbstractProject<?,?> p = (AbstractProject<?,?>) task;
            if (task instanceof MatrixConfiguration) {
                p = (AbstractProject<?,?>)((MatrixConfiguration)task).getParent();
            }
            return p.getProperty(JobPrerequisites.class);
        }
        return null;
    }


}
