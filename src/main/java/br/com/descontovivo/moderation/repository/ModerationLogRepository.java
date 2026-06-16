package br.com.descontovivo.moderation.repository;

import br.com.descontovivo.moderation.entity.ModerationLogEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

@ApplicationScoped
public class ModerationLogRepository implements PanacheRepositoryBase<ModerationLogEntity, UUID> {
}
