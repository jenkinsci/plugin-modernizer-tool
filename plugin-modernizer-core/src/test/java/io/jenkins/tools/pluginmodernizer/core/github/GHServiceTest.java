package io.jenkins.tools.pluginmodernizer.core.github;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.PluginProcessingException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.commons.util.ReflectionUtils;
import org.kohsuke.github.*;
import org.mockito.Mock;
import org.mockito.MockedStatic;
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

    @TempDir
    private Path pluginDir;

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
        assertThrows(PluginProcessingException.class, () -> {
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
        assertThrows(PluginProcessingException.class, () -> {
            service.getRepositoryFork(plugin);
        });
    }

    @Test
    public void isArchivedTest() throws Exception {
        // Mock
        GHRepository repository = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);

        doReturn(true).when(repository).isArchived();
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));

        // Test and verify
        assertTrue(service.isArchived(plugin));
        doReturn(false).when(repository).isArchived();
        assertFalse(service.isArchived(plugin));
    }

    @Test
    public void shouldFailToGetForkRepositoryInDryRunMode() throws Exception {

        // Mock
        doReturn(true).when(config).isDryRun();

        // Test
        assertThrows(PluginProcessingException.class, () -> {
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
        GHOrganization org = Mockito.mock(GHOrganization.class);

        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(org).when(github).getOrganization(eq("fake-owner"));
        doReturn(fork).when(org).getRepository(eq("fake-repo"));

        // Test and verify
        assertTrue(service.isForked(plugin));
    }

    @Test
    public void shouldNotForkInDryRunMode() throws Exception {

        // Mock
        doReturn(true).when(config).isDryRun();

        // Test
        service.fork(plugin);
        verifyNoInteractions(github);
    }

    @Test
    public void shouldNotForkArchivedRepos() throws Exception {

        GHRepository repository = Mockito.mock(GHRepository.class);

        // Mock
        doReturn(true).when(plugin).isArchived(eq(service));

        // Test
        service.fork(plugin);
        verifyNoInteractions(github);
    }

    @Test
    public void shouldForkRepoToMyself() throws Exception {

        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);

        // Mock
        doReturn("fake-repo").when(repository).getName();
        doReturn(Mockito.mock(URL.class)).when(fork).getHtmlUrl();
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(myself).when(github).getMyself();
        doReturn(fork).when(repository).fork();

        // Not yet forked
        doReturn(null).when(myself).getRepository(eq("fake-repo"));

        // Test
        service.fork(plugin);

        // Verify
        verify(repository, times(1)).fork();
    }

    @Test
    public void shouldReturnForkWhenAlreadyForkedToMyself() throws Exception {

        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);

        // Mock
        doReturn("fake-repo").when(repository).getName();
        doReturn(Mockito.mock(URL.class)).when(fork).getHtmlUrl();
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(myself).when(github).getMyself();

        // Already forked
        doReturn(fork).when(myself).getRepository(eq("fake-repo"));

        // Test
        service.fork(plugin);

        // Verify
        verify(repository, times(0)).fork();
        verify(myself, times(2)).getRepository(eq("fake-repo"));
    }

    @Test
    public void shouldForkRepoToOrganisation() throws Exception {

        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHOrganization org = Mockito.mock(GHOrganization.class);

        // Mock
        doReturn("fake-repo").when(repository).getName();
        doReturn(Mockito.mock(URL.class)).when(fork).getHtmlUrl();
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(org).when(github).getOrganization("fake-owner");
        doReturn(fork).when(repository).forkTo(eq(org));

        // Not yet forked
        doReturn(null).when(org).getRepository(eq("fake-repo"));

        // Test
        service.fork(plugin);

        // Verify
        verify(repository, times(1)).forkTo(eq(org));
    }

    @Test
    public void shouldReturnForkWhenAlreadyForkedToOrganisation() throws Exception {

        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHOrganization org = Mockito.mock(GHOrganization.class);

        // Mock
        doReturn("fake-repo").when(repository).getName();
        doReturn(Mockito.mock(URL.class)).when(fork).getHtmlUrl();
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(org).when(github).getOrganization("fake-owner");

        // Already forked to org
        doReturn(fork).when(org).getRepository(eq("fake-repo"));

        // Test
        service.fork(plugin);

        // Verify
        verify(repository, times(0)).forkTo(eq(org));
        verify(org, times(2)).getRepository(eq("fake-repo"));
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
        GHPullRequestQueryBuilder prQuery = Mockito.mock(GHPullRequestQueryBuilder.class);
        PagedIterable<?> prQueryList = Mockito.mock(PagedIterable.class);

        doReturn("fake-owner/fake-repo").when(fork).getFullName();
        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(myself).when(github).getMyself();
        doReturn(fork).when(myself).getRepository(eq("fake-repo"));
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));
        doReturn(fork).when(plugin).getRemoteForkRepository(eq(service));

        // Return at least one PR open
        doReturn(head).when(pr).getHead();
        doReturn(fork).when(head).getRepository();
        doReturn(prQuery).when(repository).queryPullRequests();
        doReturn(prQuery).when(prQuery).state(eq(GHIssueState.OPEN));
        doReturn(prQueryList).when(prQuery).list();
        doReturn(List.of(pr)).when(prQueryList).toList();

        // Test
        service.deleteFork(plugin);
        verify(fork, never()).delete();
    }

    @Test
    public void shouldNotDeleteRepoIfNotAFork() throws Exception {

        // Mock
        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);
        GHPullRequest pr = Mockito.mock(GHPullRequest.class);
        GHCommitPointer head = Mockito.mock(GHCommitPointer.class);
        GHPullRequestQueryBuilder prQuery = Mockito.mock(GHPullRequestQueryBuilder.class);
        PagedIterable<?> prQueryList = Mockito.mock(PagedIterable.class);

        doReturn(false).when(fork).isFork();
        doReturn(fork).when(github).getRepository(eq("fake-owner/fake-repo"));
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
        doReturn(prQuery).when(repository).queryPullRequests();
        doReturn(prQuery).when(prQuery).state(eq(GHIssueState.OPEN));
        doReturn(prQueryList).when(prQuery).list();
        doReturn(List.of(pr)).when(prQueryList).toList();

        // Test
        service.deleteFork(plugin);
        verify(fork, never()).delete();
    }

    @Test
    public void shouldNotDeleteRepoForkNotDetachedFromJenkinsOrg() throws Exception {

        // Mock
        GHRepository repository = Mockito.mock(GHRepository.class);
        GHRepository fork = Mockito.mock(GHRepository.class);
        GHMyself myself = Mockito.mock(GHMyself.class);
        GHPullRequest pr = Mockito.mock(GHPullRequest.class);
        GHCommitPointer head = Mockito.mock(GHCommitPointer.class);
        GHPullRequestQueryBuilder prQuery = Mockito.mock(GHPullRequestQueryBuilder.class);
        PagedIterable<?> prQueryList = Mockito.mock(PagedIterable.class);

        doReturn(true).when(fork).isFork();
        doReturn(fork).when(github).getRepository(eq("fake-owner/fake-repo"));
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
        doReturn(prQuery).when(repository).queryPullRequests();
        doReturn(prQuery).when(prQuery).state(eq(GHIssueState.OPEN));
        doReturn(prQueryList).when(prQuery).list();
        doReturn(List.of(pr)).when(prQueryList).toList();

        // Owner of the fork is jenkinsci
        doReturn(Settings.ORGANIZATION).when(fork).getOwnerName();

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
        GHPullRequestQueryBuilder prQuery = Mockito.mock(GHPullRequestQueryBuilder.class);
        PagedIterable<?> prQueryList = Mockito.mock(PagedIterable.class);

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
        doReturn(prQuery).when(repository).queryPullRequests();
        doReturn(prQuery).when(prQuery).state(eq(GHIssueState.OPEN));
        doReturn(prQueryList).when(prQuery).list();
        doReturn(List.of(pr)).when(prQueryList).toList();

        // Test
        service.deleteFork(plugin);
        verify(fork, times(1)).delete();
        verify(plugin, times(1)).withoutCommits();
        verify(plugin, times(1)).withoutChangesPushed();
    }

    @Test
    public void shouldFetchOriginalRepoInDryRunModeToNewFolder() throws Exception {

        // Mock
        GHRepository repository = Mockito.mock(GHRepository.class);
        Git git = Mockito.mock(Git.class);
        CloneCommand cloneCommand = Mockito.mock(CloneCommand.class);

        doReturn(true).when(config).isDryRun();
        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(repository).when(github).getRepository(eq("jenkinsci/fake-repo"));
        doReturn(git).when(cloneCommand).call();
        doReturn("fake-url").when(repository).getHttpTransportUrl();
        doReturn(cloneCommand).when(cloneCommand).setURI(eq("fake-url"));
        doReturn(cloneCommand).when(cloneCommand).setDirectory(any(File.class));

        // Directory doesn't exists
        doReturn(Path.of("not-existing-dir")).when(plugin).getLocalRepository();

        // Test
        try (MockedStatic<Git> mockStaticGit = mockStatic(Git.class)) {
            mockStaticGit.when(Git::cloneRepository).thenReturn(cloneCommand);
            service.fetch(plugin);
            verify(cloneCommand, times(1)).call();
            verifyNoMoreInteractions(cloneCommand);
        }
    }

    @Test
    public void shouldFetchOriginalRepoInMetaDataOnlyModeToNewFolder() throws Exception {

        // Mock
        GHRepository repository = Mockito.mock(GHRepository.class);
        Git git = Mockito.mock(Git.class);
        CloneCommand cloneCommand = Mockito.mock(CloneCommand.class);

        doReturn(false).when(config).isDryRun();
        doReturn(true).when(config).isFetchMetadataOnly();
        doReturn("fake-repo").when(plugin).getRepositoryName();
        doReturn(repository).when(github).getRepository(eq("jenkinsci/fake-repo"));
        doReturn(git).when(cloneCommand).call();
        doReturn("fake-url").when(repository).getHttpTransportUrl();
        doReturn(cloneCommand).when(cloneCommand).setURI(eq("fake-url"));
        doReturn(cloneCommand).when(cloneCommand).setDirectory(any(File.class));

        // Directory doesn't exists
        doReturn(Path.of("not-existing-dir")).when(plugin).getLocalRepository();

        // Test
        try (MockedStatic<Git> mockStaticGit = mockStatic(Git.class)) {
            mockStaticGit.when(Git::cloneRepository).thenReturn(cloneCommand);
            service.fetch(plugin);
            verify(cloneCommand, times(1)).call();
            verifyNoMoreInteractions(cloneCommand);
        }
    }

    @Test
    public void shouldOpenPullRequest() throws Exception {

        // Mocks
        GHRepository repository = Mockito.mock(GHRepository.class);
        GHPullRequest pr = Mockito.mock(GHPullRequest.class);
        GHPullRequestQueryBuilder prQuery = Mockito.mock(GHPullRequestQueryBuilder.class);
        PagedIterable<?> prQueryList = Mockito.mock(PagedIterable.class);

        doReturn(false).when(config).isDraft();
        doReturn(true).when(plugin).hasChangesPushed();
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));

        // Return no open PR
        doReturn(prQuery).when(repository).queryPullRequests();
        doReturn(prQuery).when(prQuery).state(eq(GHIssueState.OPEN));
        doReturn(prQueryList).when(prQuery).list();
        doReturn(List.of()).when(prQueryList).toList();

        doReturn(pr)
                .when(repository)
                .createPullRequest(anyString(), anyString(), isNull(), anyString(), eq(false), eq(false));

        // Test
        service.openPullRequest(plugin);
    }

    @Test
    public void shouldOpenDraftPullRequest() throws Exception {

        // Mocks
        GHRepository repository = Mockito.mock(GHRepository.class);
        GHPullRequest pr = Mockito.mock(GHPullRequest.class);
        GHPullRequestQueryBuilder prQuery = Mockito.mock(GHPullRequestQueryBuilder.class);
        PagedIterable<?> prQueryList = Mockito.mock(PagedIterable.class);

        doReturn(true).when(config).isDraft();
        doReturn(true).when(plugin).hasChangesPushed();
        doReturn(repository).when(plugin).getRemoteRepository(eq(service));

        // Return no open PR
        doReturn(prQuery).when(repository).queryPullRequests();
        doReturn(prQuery).when(prQuery).state(eq(GHIssueState.OPEN));
        doReturn(prQueryList).when(prQuery).list();
        doReturn(List.of()).when(prQueryList).toList();

        doReturn(pr)
                .when(repository)
                .createPullRequest(anyString(), anyString(), isNull(), anyString(), eq(false), eq(true));

        // Test
        service.openPullRequest(plugin);
    }
}
