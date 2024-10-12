package io.jenkins.tools.pluginmodernizer.core;

import com.google.inject.AbstractModule;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import io.jenkins.tools.pluginmodernizer.core.impl.CacheManager;
import io.jenkins.tools.pluginmodernizer.core.impl.PluginModernizer;
import io.jenkins.tools.pluginmodernizer.core.utils.JdkFetcher;
import io.jenkins.tools.pluginmodernizer.core.utils.PluginService;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.Invoker;

public class GuiceModule extends AbstractModule {

    private final Config config;

    public GuiceModule(Config config) {
        this.config = config;
    }

    @Override
    protected void configure() {
        bind(Invoker.class).to(DefaultInvoker.class);
        bind(Config.class).toInstance(config);
        bind(CacheManager.class).toInstance(new CacheManager(config.getCachePath()));
        bind(PluginService.class).toInstance(new PluginService());
        bind(GHService.class).toInstance(new GHService());
        bind(JdkFetcher.class).toInstance(new JdkFetcher(config.getCachePath()));
        bind(PluginModernizer.class).toInstance(new PluginModernizer());
    }
}
