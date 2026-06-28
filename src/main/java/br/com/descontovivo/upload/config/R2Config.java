package br.com.descontovivo.upload.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "r2")
public interface R2Config {

    String endpoint();

    String region();

    @WithName("access-key-id")
    String accessKeyId();

    @WithName("secret-access-key")
    String secretAccessKey();

    String bucket();

    @WithName("public-base-url")
    String publicBaseUrl();
}
