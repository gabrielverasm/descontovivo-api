package br.com.descontovivo.account.repository;

import br.com.descontovivo.account.entity.AccountDataRequestEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AccountDataRequestRepository implements PanacheRepositoryBase<AccountDataRequestEntity, UUID> {

    public List<AccountDataRequestEntity> findByUserSubject(String userSubject) {
        return list("userSubject", Sort.by("createdAt").descending(), userSubject);
    }
}
