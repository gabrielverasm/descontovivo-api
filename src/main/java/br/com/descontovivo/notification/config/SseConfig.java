package br.com.descontovivo.notification.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "notification.sse")
public interface SseConfig {

    @WithName("interval-seconds")
    @WithDefault("30")
    int intervalSeconds();
}
