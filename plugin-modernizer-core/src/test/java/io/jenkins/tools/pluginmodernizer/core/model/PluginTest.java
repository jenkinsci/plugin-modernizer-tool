package io.jenkins.tools.pluginmodernizer.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import io.jenkins.tools.pluginmodernizer.core.impl.MavenInvoker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

@ExtendWith(MockitoExtension.class)
public class PluginTest {

    @Mock
    private MavenInvoker mavenInvoker;

    @Mock
    private GHService ghService;

    @Test
    public void testPluginName() {
        Plugin plugin = Plugin.build("example");
        assertEquals("example", plugin.getName());
        plugin.withName("new-name");
        assertEquals("new-name", plugin.getName());
    }

    @Test
    public void testRepositoryName() {
        Plugin plugin = Plugin.build("example");
        assertNull(plugin.getRepositoryName());
        plugin.withRepositoryName("new-repo");
        assertEquals("new-repo", plugin.getRepositoryName());
    }

    @Test
    public void testLocalRepository() {
        Plugin plugin = Plugin.build("example");
        assertEquals(
                Path.of(Settings.TEST_PLUGINS_DIRECTORY, plugin.getName()).toString(),
                plugin.getLocalRepository().toString());
    }

    @Test
    public void testGetGitHubRepository() {
        Plugin plugin = Plugin.build("example");
        plugin.withRepositoryName("repo-name");
        assertEquals(
                "https://github.com/foobar/repo-name.git",
                plugin.getGitRepositoryURI("foobar").toString());
    }

    @Test
    public void testHasCommits() {
        Plugin plugin = Plugin.build("example");
        assertFalse(plugin.hasCommits());
        plugin.withCommits();
        assertTrue(plugin.hasCommits());
        plugin.withoutCommits();
        assertFalse(plugin.hasCommits());
    }

    @Test
    public void testClean() {
        Plugin plugin = Plugin.build("example");
        plugin.clean(mavenInvoker);
        verify(mavenInvoker).invokeGoal(plugin, "clean");
        verifyNoMoreInteractions(mavenInvoker);
    }

    @Test
    public void testCompile() {
        Plugin plugin = Plugin.build("example");
        plugin.compile(mavenInvoker);
        verify(mavenInvoker).invokeGoal(plugin, "compile");
        verifyNoMoreInteractions(mavenInvoker);
    }

    @Test
    public void testVerify() {
        Plugin plugin = Plugin.build("example");
        plugin.verify(mavenInvoker);
        verify(mavenInvoker).invokeGoal(plugin, "verify");
        verifyNoMoreInteractions(mavenInvoker);
    }

    @Test
    public void testRewrite() {
        Plugin plugin = Plugin.build("example");
        plugin.runOpenRewrite(mavenInvoker);
        verify(mavenInvoker).invokeRewrite(plugin);
        verifyNoMoreInteractions(mavenInvoker);
    }

    @Test
    public void testFork() {
        Plugin plugin = Plugin.build("example");
        plugin.fork(ghService);
        verify(ghService).fork(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testIsFork() {
        Plugin plugin = Plugin.build("example");
        plugin.isForked(ghService);
        verify(ghService).isForked(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testDeleteFork() {
        Plugin plugin = Plugin.build("example");
        plugin.deleteFork(ghService);
        verify(ghService).deleteFork(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testIsArchived() {
        Plugin plugin = Plugin.build("example");
        plugin.isArchived(ghService);
        verify(ghService).isArchived(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testCheckoutBranch() {
        Plugin plugin = Plugin.build("example");
        plugin.checkoutBranch(ghService);
        verify(ghService).checkoutBranch(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testCommit() {
        Plugin plugin = Plugin.build("example");
        plugin.commit(ghService);
        verify(ghService).commitChanges(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testPush() {
        Plugin plugin = Plugin.build("example");
        plugin.push(ghService);
        verify(ghService).pushChanges(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testOpenPullRequest() {
        Plugin plugin = Plugin.build("example");
        plugin.openPullRequest(ghService);
        verify(ghService).openPullRequest(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testFetch() {
        Plugin plugin = Plugin.build("example");
        plugin.fetch(ghService);
        verify(ghService).fetch(plugin);
        verifyNoMoreInteractions(ghService);
    }

    @Test
    public void testGetRemoteRepository() {
        Plugin plugin = Plugin.build("example");
        plugin.withRepositoryName("repo-name");
        plugin.getRemoteRepository(ghService);
        verify(ghService).getRepository(plugin);
    }

    @Test
    public void testGetRemoteForkRepository() {
        Plugin plugin = Plugin.build("example");
        plugin.withRepositoryName("repo-name");
        plugin.getRemoteForkRepository(ghService);
        verify(ghService).getRepositoryFork(plugin);
    }

    @Test
    public void testHasErrors() {
        Plugin plugin = Plugin.build("example").withConfig(mock(Config.class));
        assertFalse(plugin.hasErrors());
        plugin.addError("error", new Exception("error"));
        assertTrue(plugin.hasErrors());
    }

    @Test
    public void testToString() {
        Plugin plugin = Plugin.build("example");
        assertEquals("example", plugin.toString());
    }

    @Test
    public void testGetMarker() {
        Plugin plugin = Plugin.build("example");
        Marker expectedMarker = MarkerFactory.getMarker("example");
        Marker actualMarker = plugin.getMarker();
        assertEquals(expectedMarker, actualMarker);
    }

    @Test
    public void testRemoveLocalData_SuccessfulDeletion() throws IOException {
        Plugin plugin = Plugin.build("example");
        // Create a temporary directory that will be used as the local repository
        Path tempDir = Path.of(Settings.TEST_PLUGINS_DIRECTORY + "example");
        Files.createDirectories(tempDir);
        assertTrue(tempDir.toFile().exists());
        plugin.removeLocalData();
        assertFalse(tempDir.toFile().exists());
    }

    @Test
    void testRemoveLocalData_DirectoryDoesNotExist() {
        Plugin plugin = Plugin.build("Not-Exist-example");
        Path tempDir = Path.of(Settings.TEST_PLUGINS_DIRECTORY + "Not-Exist-example");
        assertFalse(tempDir.toFile().exists());
        plugin.removeLocalData();
        assertFalse(tempDir.toFile().exists());
    }

    @Test
    void testRemoveLocalData_FailureToDeleteDirectory() throws IOException {
        Plugin plugin = Plugin.build("example");
        Path tempDir = Path.of(Settings.TEST_PLUGINS_DIRECTORY + "example");
        Files.createDirectories(tempDir);
        try (MockedStatic<FileUtils> mockedFileUtils = mockStatic(FileUtils.class)) {
            mockedFileUtils
                    .when(() -> FileUtils.deleteDirectory(tempDir.toFile()))
                    .thenThrow(new IOException("Forced IO exception"));

            assertThrows(IllegalArgumentException.class, plugin::removeLocalData);
            assertTrue(tempDir.toFile().exists());
        } finally {
            if (Files.exists(tempDir)) {
                Files.delete(tempDir);
            }
        }
    }

    @Test
    void testRemoveLocalData_PathIsFile() throws IOException {
        Plugin plugin = Plugin.build("example");
        Path tempFile = Path.of(Settings.TEST_PLUGINS_DIRECTORY + "example");
        Files.createFile(tempFile);
        try {
            plugin.removeLocalData();
            assertTrue(tempFile.toFile().exists());
        } finally {
            if (Files.exists(tempFile)) {
                Files.delete(tempFile);
            }
        }
    }
}
