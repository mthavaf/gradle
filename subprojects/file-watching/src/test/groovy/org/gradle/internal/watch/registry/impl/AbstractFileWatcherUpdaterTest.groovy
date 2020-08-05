/*
 * Copyright 2020 the original author or authors.
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

package org.gradle.internal.watch.registry.impl

import net.rubygrapefruit.platform.file.FileWatcher
import org.gradle.api.internal.cache.StringInterner
import org.gradle.api.internal.file.TestFiles
import org.gradle.internal.file.FileMetadata.AccessType
import org.gradle.internal.file.impl.DefaultFileMetadata
import org.gradle.internal.snapshot.CaseSensitivity
import org.gradle.internal.snapshot.CompleteDirectorySnapshot
import org.gradle.internal.snapshot.CompleteFileSystemLocationSnapshot
import org.gradle.internal.snapshot.RegularFileSnapshot
import org.gradle.internal.snapshot.SnapshotHierarchy
import org.gradle.internal.snapshot.impl.DirectorySnapshotter
import org.gradle.internal.vfs.VirtualFileSystem
import org.gradle.internal.vfs.impl.DefaultSnapshotHierarchy
import org.gradle.internal.watch.registry.FileWatcherUpdater
import org.gradle.internal.watch.registry.SnapshotCollectingDiffListener
import org.gradle.test.fixtures.file.CleanupTestDirectory
import org.gradle.test.fixtures.file.TestFile
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Predicate

@CleanupTestDirectory
abstract class AbstractFileWatcherUpdaterTest extends Specification {
    @Rule
    TestNameTestDirectoryProvider temporaryFolder = new TestNameTestDirectoryProvider(getClass())

    def watcher = Mock(FileWatcher)
    def ignoredForWatching = [] as Set<String>
    Predicate<String> watchFilter = { path -> !ignoredForWatching.contains(path) }
    def directorySnapshotter = new DirectorySnapshotter(TestFiles.fileHasher(), new StringInterner(), [])
    FileWatcherUpdater updater
    def virtualFileSystem = new VirtualFileSystem() {
        private SnapshotHierarchy root = DefaultSnapshotHierarchy.empty(CaseSensitivity.CASE_SENSITIVE)
        @Override
        SnapshotHierarchy getRoot() {
            return root
        }

        @Override
        void update(VirtualFileSystem.UpdateFunction updateFunction) {
            def diffListener = new SnapshotCollectingDiffListener()
            root = updateFunction.update(root, diffListener)
            diffListener.publishSnapshotDiff(updater, root)
        }
    }

    def setup() {
        updater = createUpdater(watcher, watchFilter)
    }

    abstract FileWatcherUpdater createUpdater(FileWatcher watcher, Predicate<String> watchFilter)

    def "does not watch directories outside of hierarchies to watch"() {
        def watchableHierarchies = ["first", "second", "third"].collect { file(it).createDir() }
        def fileOutsideOfWatchableHierarchies = file("forth").file("someFile.txt")

        when:
        registerWatchableHierarchies(watchableHierarchies)
        then:
        0 * _

        when:
        fileOutsideOfWatchableHierarchies.text = "hello"
        addSnapshot(snapshotRegularFile(fileOutsideOfWatchableHierarchies))
        then:
        0 * _
        vfsHasSnapshotsAt(fileOutsideOfWatchableHierarchies)

        when:
        buildFinished()
        then:
        0 * _
        !vfsHasSnapshotsAt(fileOutsideOfWatchableHierarchies)
    }

    def "retains files in hierarchies ignored for watching"() {
        def watchableHierarchy = file("watchable").createDir()
        def fileOutsideOfWatchableHierarchy = file("outside").file("someFile.txt").createFile()
        def fileInDirectoryIgnoredForWatching = file("cache/some-cache/someFile.txt").createFile()
        ignoredForWatching.add(fileInDirectoryIgnoredForWatching.absolutePath)

        when:
        registerWatchableHierarchies([watchableHierarchy])
        then:
        0 * _

        when:
        addSnapshot(snapshotRegularFile(fileOutsideOfWatchableHierarchy))
        addSnapshot(snapshotRegularFile(fileInDirectoryIgnoredForWatching))
        then:
        0 * _
        vfsHasSnapshotsAt(fileOutsideOfWatchableHierarchy)
        vfsHasSnapshotsAt(fileInDirectoryIgnoredForWatching)

        when:
        buildFinished()
        then:
        0 * _
        !vfsHasSnapshotsAt(fileOutsideOfWatchableHierarchy)
        vfsHasSnapshotsAt(fileInDirectoryIgnoredForWatching)
    }

    def "fails when discovering a hierarchy to watch and there is already something in the VFS"() {
        def watchableHierarchy = file("watchable").createDir()
        def fileInWatchableHierarchy = watchableHierarchy.file("some/dir/file.txt").createFile()

        when:
        addSnapshot(snapshotRegularFile(fileInWatchableHierarchy))
        then:
        0 * _

        when:
        registerWatchableHierarchies([watchableHierarchy])
        then:
        def exception = thrown(RuntimeException)
        exception.message == "Found existing snapshot at '${fileInWatchableHierarchy.absolutePath}' for unwatched hierarchy '${watchableHierarchy.absolutePath}'"
    }

    def "does not watch symlinks and removes symlinks at the end of the build"() {
        def watchableHierarchy = file("watchable").createDir()
        def symlinkInWatchableHierarchy = watchableHierarchy.file("some/dir/file.txt").createFile()

        when:
        registerWatchableHierarchies([watchableHierarchy])
        addSnapshot(snapshotSymlinkedFile(symlinkInWatchableHierarchy))
        then:
        vfsHasSnapshotsAt(symlinkInWatchableHierarchy)
        0 * _

        when:
        buildFinished()
        then:
        !vfsHasSnapshotsAt(symlinkInWatchableHierarchy)
        0 * _
    }

    def "does not watch ignored files in a hierarchy to watch"() {
        def watchableHierarchy = file("watchable").createDir()
        def ignoredFileInHierarchy = watchableHierarchy.file("caches/cacheFile").createFile()
        ignoredForWatching.add(ignoredFileInHierarchy.absolutePath)
        ignoredForWatching.add(ignoredFileInHierarchy.parentFile.absolutePath)

        when:
        registerWatchableHierarchies([watchableHierarchy])
        addSnapshot(snapshotRegularFile(ignoredFileInHierarchy))
        then:
        vfsHasSnapshotsAt(ignoredFileInHierarchy)
        0 * _

        when:
        buildFinished()
        then:
        vfsHasSnapshotsAt(ignoredFileInHierarchy)
        0 * _
    }

    def "fails when hierarchy to watch is ignored"() {
        def watchableHierarchy = file("watchable").createDir()
        ignoredForWatching.add(watchableHierarchy.absolutePath)

        when:
        registerWatchableHierarchies([watchableHierarchy])
        then:
        def exception = thrown(RuntimeException)
        exception.message == "Unable to watch directory '${watchableHierarchy.absolutePath}' since it is within Gradle's caches"
    }

    TestFile file(Object... path) {
        temporaryFolder.testDirectory.file(path)
    }

    CompleteDirectorySnapshot snapshotDirectory(File directory) {
        directorySnapshotter.snapshot(directory.absolutePath, null, new AtomicBoolean(false)) as CompleteDirectorySnapshot
    }

    void addSnapshot(CompleteFileSystemLocationSnapshot snapshot) {
        virtualFileSystem.update({ currentRoot, listener -> currentRoot.store(snapshot.absolutePath, snapshot, listener) })
    }

    void invalidate(String absolutePath) {
        virtualFileSystem.update({ currentRoot, listener -> currentRoot.invalidate(absolutePath, listener) })
    }

    void invalidate(CompleteFileSystemLocationSnapshot snapshot) {
        invalidate(snapshot.absolutePath)
    }

    static RegularFileSnapshot snapshotRegularFile(File regularFile) {
        def attributes = Files.readAttributes(regularFile.toPath(), BasicFileAttributes)
        new RegularFileSnapshot(
            regularFile.absolutePath,
            regularFile.name,
            TestFiles.fileHasher().hash(regularFile),
            DefaultFileMetadata.file(attributes.lastModifiedTime().toMillis(), attributes.size(), AccessType.DIRECT)
        )
    }

    static RegularFileSnapshot snapshotSymlinkedFile(File regularFile) {
        def attributes = Files.readAttributes(regularFile.toPath(), BasicFileAttributes)
        new RegularFileSnapshot(
            regularFile.absolutePath,
            regularFile.name,
            TestFiles.fileHasher().hash(regularFile),
            DefaultFileMetadata.file(attributes.lastModifiedTime().toMillis(), attributes.size(), AccessType.VIA_SYMLINK)
        )
    }

    static boolean equalIgnoringOrder(Object actual, Collection<?> expected) {
        List<?> actualSorted = (actual as List).toSorted()
        List<?> expectedSorted = (expected as List).toSorted()
        return actualSorted == expectedSorted
    }

    boolean vfsHasSnapshotsAt(File location) {
        def visitor = new CheckIfNonEmptySnapshotVisitor()
        virtualFileSystem.root.visitSnapshotRoots(location.absolutePath, visitor)
        return !visitor.empty
    }

    void registerWatchableHierarchies(Iterable<File> watchableHierarchies) {
        watchableHierarchies.each { watchableHierarchy ->
            virtualFileSystem.update { root, diffListener ->
                updater.registerWatchableHierarchy(watchableHierarchy, root)
                return root
            }
        }
    }

    void buildFinished() {
        virtualFileSystem.update { root, diffListener ->
            updater.buildFinished(root)
        }
    }

    private static class CheckIfNonEmptySnapshotVisitor implements SnapshotHierarchy.SnapshotVisitor {

        boolean empty = true

        @Override
        void visitSnapshotRoot(CompleteFileSystemLocationSnapshot snapshot) {
            empty = false
        }
    }
}
