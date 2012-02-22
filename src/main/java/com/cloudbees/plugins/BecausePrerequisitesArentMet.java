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

import hudson.model.Node;
import hudson.model.queue.CauseOfBlockage;

/**
 * Cause of blockage to track a node can't execute a job because the prerequisites this ones define aren't met
 *
 * @author: <a hef="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class BecausePrerequisitesArentMet extends CauseOfBlockage {

    public final Node node;

    public BecausePrerequisitesArentMet(Node node) {
        this.node = node;
    }

    @Override
    public String getShortDescription() {
        return "Job prerequisites are not met";
    }
}
