package br.com.descontovivo.shared.api;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record VersionResponse(String name, String version) {
}
