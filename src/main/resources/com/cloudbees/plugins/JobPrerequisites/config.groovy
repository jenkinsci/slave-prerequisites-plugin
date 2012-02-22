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

package com.cloudbees.plugins.JobPrerequisites

def f=namespace(lib.FormTagLib)

f.optionalBlock(title:_("Check job prerequisites"), name:"prerequisites",
        checked:instance!=null, help:"/plugin/slave-prerequisites/help.html") {
    f.nested {
        f.entry(field:"script") {
            f.textarea()
        }
    }
}