package io.jenkins.tools.pluginmodernizer.core.github;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.ReflectionUtils;
import org.kohsuke.github.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith({MockitoExtension.class})
public class GHServiceTest {

    @Mock
    private Config config;

    @Mock
    private Plugin plugin;

    @Mock
    private GitHub github;

    /**
     * Tested instance
     */
    private GHService service;

    @BeforeEach
    public void setup() throws Exception {

        doReturn("fake-owner").when(config).getGithubOwner();

        // Create service
        service = new GHService(config);

        // Set github mock
        Field field = ReflectionUtils.findFields(
                        GHService.class,
                        f -> f.getName().equals("github"),
                        ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
                .get(0);
        field.setAccessible(true);
        field.set(service, github);
    }

    @Test
    public void shouldGetRepository() throws Exception {

        // Mock
        GHRepository mock = Mockito.mock(GHRepository.class);
        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(mock).when(github).getRepository(eq("jenkinsci/fake-repo"));

        // Test
        GHRepository repository = service.getRepository(plugin);

        // Verify
        assertSame(mock, repository);
    }

    @Test
    public void shouldFailToGetRepository() throws Exception {

        // Mock
        doReturn("fake-repo").when(plugin).getRepositoryName();
        doThrow(new IOException()).when(github).getRepository(eq("jenkinsci/fake-repo"));

        // Test
        assertThrows(IllegalArgumentException.class, () -> {
            service.getRepository(plugin);
        });
    }

    @Test
    public void shouldGetRepositoryFork() throws Exception {

        // Mock
        GHRepository mock = Mockito.mock(GHRepository.class);
        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(mock).when(github).getRepository(eq("fake-owner/fake-repo"));

        // Test
        GHRepository repository = service.getRepositoryFork(plugin);

        // Verify
        assertSame(mock, repository);
    }

    @Test
    public void shouldFailToGetForkRepository() throws Exception {

        // Mock
        doReturn("fake-repo").when(plugin).getRepositoryName();
        doThrow(new IOException()).when(github).getRepository(eq("fake-owner/fake-repo"));

        // Test
        assertThrows(IllegalArgumentException.class, () -> {
            service.getRepositoryFork(plugin);
        });
    }

    @Test
    public void shouldFailToGetForkRepositoryInDryRunMode() throws Exception {

        // Mock
        doReturn(true).when(config).isDryRun();

        // Test
        assertThrows(IllegalArgumentException.class, () -> {
            service.getRepositoryFork(plugin);
        });
    }

    @Test
    public void isForkedTest() throws Exception {

        // Mock
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);

        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(myself).when(github).getMyself();
        doReturn(fork).when(myself).getRepository(eq("fake-repo"));

        // Test and verify
        assertTrue(service.isForked(plugin));
    }

    @Test
    public void isNotForkedTest() throws Exception {

        // Mock
        GHMyself myself = Mockito.mock(GHMyself.class);

        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(myself).when(github).getMyself();
        doReturn(null).when(myself).getRepository(eq("fake-repo"));

        // Test and verify
        assertFalse(service.isForked(plugin));
    }

    @Test
    public void isForkedToOrganisation() throws Exception {

        // Mock
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);
        GHOrganization org = Mockito.mock(GHOrganization.class);

        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(myself).when(github).getMyself();
        doReturn(null).when(myself).getRepository(eq("fake-repo"));
        doReturn(org).when(github).getOrganization(eq("fake-owner"));
        doReturn(fork).when(org).getRepository(eq("fake-repo"));

        // Test and verify
        assertTrue(service.isForked(plugin));
    }

    @Test
    public void shouldNotDeleteForkIsDryRunMode() throws Exception {

        // Mock
        GHRepository fork = Mockito.mock(GHRepository.class);

        doReturn(true).when(config).isDryRun();

        // Test
        service.deleteFork(plugin);
        verifyNoInteractions(github);
        verify(fork, never()).delete();
    }

    @Test
    public void shouldNotAttemptToDeleteNonFork() throws Exception {

        // Mock
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);

        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(myself).when(github).getMyself();
        doReturn(null).when(myself).getRepository(eq("fake-repo"));

        // Test
        service.deleteFork(plugin);
        verify(fork, never()).delete();
    }

    @Test
    public void shouldNotDeleteForkWithOpenPullRequestSource() throws Exception {

        // Mock
        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);
        GHPullRequest pr = Mockito.mock(GHPullRequest.class);
        GHCommitPointer head = Mockito.mock(GHCommitPointer.class);

        doReturn("fake-owner/fake-repo").when(fork).getFullName();
        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(myself).when(github).getMyself();
        doReturn(fork).when(myself).getRepository(eq("fake-repo"));
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(fork).when(plugin).getRemoteForkRepository(eq(service));

        // Return at least one PR open
        doReturn(head).when(pr).getHead();
        doReturn(fork).when(head).getRepository();
        doReturn(List.of(pr)).when(repository).getPullRequests(eq(GHIssueState.OPEN));

        // Test
        service.deleteFork(plugin);
        verify(fork, never()).delete();
    }

    @Test
    public void shouldDeleteForkIfAllConditionsMet() throws Exception {

        // Mock
        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);
        GHPullRequest pr = Mockito.mock(GHPullRequest.class);
        GHCommitPointer head = Mockito.mock(GHCommitPointer.class);

        doReturn(true).when(fork).isFork();
        doReturn(fork).when(github).getRepository(eq("fake-owner/fake-repo"));
        doReturn("fake-owner").when(fork).getOwnerName();
        doReturn("fake-owner/fake-repo").when(fork).getFullName();
        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(myself).when(github).getMyself();
        doReturn(fork).when(myself).getRepository(eq("fake-repo"));
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(fork).when(plugin).getRemoteForkRepository(eq(service));

        // One PR open but not from the fork
        doReturn(head).when(pr).getHead();
        GHRepository otherFork = Mockito.mock(GHRepository.class);
        doReturn("an/other").when(otherFork).getFullName();
        doReturn(otherFork).when(head).getRepository();
        doReturn(List.of(pr)).when(repository).getPullRequests(eq(GHIssueState.OPEN));

        // Test
        service.deleteFork(plugin);
        verify(fork, times(1)).delete();
    }
}
