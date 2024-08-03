package io.jenkins.tools.pluginmodernizer.core.github;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import java.io.IOException;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.ReflectionUtils;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
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
}
