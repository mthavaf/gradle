/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.api.file;

import org.gradle.api.GradleException;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class LinksStrategy implements Serializable {
    /**
     * Do not preserve any symlinks. This is the default.
     */
    public static final LinksStrategy NONE = new LinksStrategy();
    /**
     * Preserve relative symlinks.
     */
    public static final LinksStrategy RELATIVE = new LinksStrategy(false, true, false);
    /**
     * Preserve all symlinks, even if they point to non-existent paths.
     */
    public static final LinksStrategy ALL = new LinksStrategy(true, true, true);

    private final boolean preserveAbsolute;
    private final boolean preserveRelative;
    private final boolean preserveBroken;

    public LinksStrategy() {
        this.preserveAbsolute = false;
        this.preserveRelative = false;
        this.preserveBroken = false;
    }

    public LinksStrategy(boolean preserveAbsolute, boolean preserveRelative, boolean preserveBroken) {
        this.preserveAbsolute = preserveAbsolute;
        this.preserveRelative = preserveRelative;
        this.preserveBroken = preserveBroken;
    }

    public boolean getPreserveAbsolute() {
        return preserveAbsolute;
    }

    public boolean getPreserveRelative() {
        return preserveRelative;
    }

    public boolean getPreserveBroken() {
        return preserveBroken;
    }

    public boolean preserveAny() {
        return preserveAbsolute || preserveRelative || preserveBroken;
    }

    public boolean shouldBePreserved(Path path) { //FIXME: should be more generic for arhives support
        if (!Files.isSymbolicLink(path)) {
            return false;
        }
        Path linkTarget;
        try {
            linkTarget = Files.readSymbolicLink(path);
        } catch (IOException e) {
            throw new GradleException(String.format("Couldn't read symbolic link '%s'.", path), e);
        }
        //FIXME: fails during unzip because link can be unpacked earlier than target
//        if (!preserveBroken && !Files.exists(path)) {
//            throw new GradleException(String.format("Couldn't follow symbolic link '%s'.", path));
//        }
        boolean isAbsolute = linkTarget.isAbsolute();
        return (isAbsolute && preserveAbsolute) || (!isAbsolute && preserveRelative);
    }

    public boolean shouldBePreserved(FileTreeElement path) { //FIXME: should be more generic for arhives support
        if (!path.isSymbolicLink()) {
            return false;
        }
        String target = path.getSymbolicLinkTarget();
        //FIXME: fails during unzip because link can be unpacked earlier than target
//        if (!preserveBroken && !Files.exists(path)) {
//            throw new GradleException(String.format("Couldn't follow symbolic link '%s'.", path));
//        }
        boolean isAbsolute = target.startsWith("/"); //FIXME
        return (isAbsolute && preserveAbsolute) || (!isAbsolute && preserveRelative);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LinksStrategy that = (LinksStrategy) o;
        return preserveAbsolute == that.preserveAbsolute && preserveRelative == that.preserveRelative && preserveBroken == that.preserveBroken;
    }

    @Override
    public int hashCode() {
        return Objects.hash(preserveAbsolute, preserveRelative, preserveBroken);
    }
}
