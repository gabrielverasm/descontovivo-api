package br.com.descontovivo.account.repository;

import br.com.descontovivo.account.entity.AccountDataRequestEntity;
import br.com.descontovivo.account.entity.DataRequestStatus;
import br.com.descontovivo.account.entity.DataRequestType;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class AccountDataRequestRepository implements PanacheRepositoryBase<AccountDataRequestEntity, UUID> {

    public List<AccountDataRequestEntity> findByUserSubject(String userSubject) {
        return list("userSubject", Sort.by("createdAt").descending(), userSubject);
    }

    public List<AccountDataRequestEntity> findForAdmin(DataRequestStatus status,
                                                       DataRequestType type,
                                                       String userSubject,
                                                       int page,
                                                       int size) {
        var conditions = new ArrayList<String>();
        var params = new HashMap<String, Object>();

        if (status != null) {
            conditions.add("status = :status");
            params.put("status", status);
        }
        if (type != null) {
            conditions.add("requestType = :type");
            params.put("type", type);
        }
        if (userSubject != null && !userSubject.isBlank()) {
            conditions.add("userSubject = :userSubject");
            params.put("userSubject", userSubject.trim());
        }

        int effectiveSize = Math.min(size, 100);

        if (conditions.isEmpty()) {
            return findAll(Sort.by("createdAt").descending())
                    .page(page, effectiveSize)
                    .list();
        }

        String query = String.join(" AND ", conditions);
        return find(query, Sort.by("createdAt").descending(), params)
                .page(page, effectiveSize)
                .list();
    }

    /**
     * Count data requests with status PENDING or IN_REVIEW (i.e., "open" requests).
     */
    public long countOpenRequests() {
        return count("status = ?1 or status = ?2", DataRequestStatus.PENDING, DataRequestStatus.IN_REVIEW);
    }
}
