package br.com.descontovivo.shared.api;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record SystemInfoResponse(String application, String status) {
}
