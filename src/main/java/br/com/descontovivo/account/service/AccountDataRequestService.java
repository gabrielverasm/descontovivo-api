package br.com.descontovivo.account.service;

import br.com.descontovivo.account.api.AccountDataRequestResponse;
import br.com.descontovivo.account.api.AccountDataRequestSummary;
import br.com.descontovivo.account.api.admin.AccountDataRequestAdminSummary;
import br.com.descontovivo.account.api.admin.AccountDataRequestAdminUpdate;
import br.com.descontovivo.account.entity.AccountDataRequestEntity;
import br.com.descontovivo.account.entity.DataRequestStatus;
import br.com.descontovivo.account.entity.DataRequestType;
import br.com.descontovivo.account.repository.AccountDataRequestRepository;
import br.com.descontovivo.shared.security.CurrentUserProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class AccountDataRequestService {

    private static final String SUCCESS_MESSAGE =
            "Solicitação registrada. Avaliaremos o pedido conforme a legislação aplicável e as necessidades de segurança e manutenção de registros permitidos.";

    private static final Set<DataRequestStatus> TERMINAL_STATUSES = Set.of(
            DataRequestStatus.COMPLETED, DataRequestStatus.REJECTED);

    private final AccountDataRequestRepository repository;
    private final CurrentUserProvider currentUserProvider;

    public AccountDataRequestService(AccountDataRequestRepository repository,
                                     CurrentUserProvider currentUserProvider) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public AccountDataRequestResponse create(String type, String details) {
        var user = currentUserProvider.currentUser();
        var requestType = parseType(type);

        var entity = new AccountDataRequestEntity();
        entity.setUserSubject(user.subject());
        entity.setUsername(user.username());
        entity.setEmail(user.email());
        entity.setDisplayName(user.name());
        entity.setRequestType(requestType);
        entity.setDetails(details != null ? details.trim() : null);
        entity.setStatus(DataRequestStatus.PENDING);
        entity.setCreatedAt(OffsetDateTime.now());

        repository.persist(entity);

        return new AccountDataRequestResponse(
                entity.getId(),
                entity.getRequestType().name(),
                entity.getStatus().name(),
                entity.getCreatedAt(),
                SUCCESS_MESSAGE
        );
    }

    public List<AccountDataRequestSummary> listMine() {
        var user = currentUserProvider.currentUser();
        return repository.findByUserSubject(user.subject()).stream()
                .map(e -> new AccountDataRequestSummary(
                        e.getId(),
                        e.getRequestType().name(),
                        e.getStatus().name(),
                        e.getCreatedAt(),
                        e.getResolvedAt()
                ))
                .toList();
    }

    // --- Admin methods ---

    public List<AccountDataRequestAdminSummary> listForAdmin(String status, String type,
                                                              String userSubject,
                                                              int page, int size) {
        DataRequestStatus statusFilter = parseStatusFilter(status);
        DataRequestType typeFilter = parseTypeFilter(type);

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));

        return repository.findForAdmin(statusFilter, typeFilter, userSubject, safePage, safeSize).stream()
                .map(this::toAdminSummary)
                .toList();
    }

    @Transactional
    public AccountDataRequestAdminSummary updateStatus(UUID id, AccountDataRequestAdminUpdate request) {
        var entity = repository.findById(id);
        if (entity == null) {
            throw new NotFoundException("Data request not found: " + id);
        }

        DataRequestStatus currentStatus = entity.getStatus();
        DataRequestStatus newStatus = parseStatusStrict(request.status());

        // Do not allow reverting from terminal statuses
        if (TERMINAL_STATUSES.contains(currentStatus)) {
            throw new BadRequestException(
                    "Solicitação já está em status final (" + currentStatus + "). Não é possível alterar.");
        }

        entity.setStatus(newStatus);
        entity.setUpdatedAt(OffsetDateTime.now());

        if (TERMINAL_STATUSES.contains(newStatus)) {
            entity.setResolvedAt(OffsetDateTime.now());
        }

        if (request.resolutionNote() != null && !request.resolutionNote().isBlank()) {
            entity.setResolutionNote(request.resolutionNote().trim());
        }

        return toAdminSummary(entity);
    }

    // --- Private helpers ---

    private AccountDataRequestAdminSummary toAdminSummary(AccountDataRequestEntity e) {
        return new AccountDataRequestAdminSummary(
                e.getId(),
                e.getUserSubject(),
                e.getUsername(),
                e.getEmail(),
                e.getDisplayName(),
                e.getRequestType().name(),
                e.getStatus().name(),
                e.getDetails(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getResolvedAt(),
                e.getResolutionNote()
        );
    }

    private DataRequestType parseType(String type) {
        if (type == null || type.isBlank()) {
            throw new BadRequestException("Tipo de solicitação é obrigatório.");
        }
        try {
            return DataRequestType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Tipo de solicitação inválido: " + type);
        }
    }

    private DataRequestStatus parseStatusFilter(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return DataRequestStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Status inválido: " + status);
        }
    }

    private DataRequestType parseTypeFilter(String type) {
        if (type == null || type.isBlank()) return null;
        try {
            return DataRequestType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Tipo inválido: " + type);
        }
    }

    private DataRequestStatus parseStatusStrict(String status) {
        if (status == null || status.isBlank()) {
            throw new BadRequestException("Status é obrigatório.");
        }
        try {
            return DataRequestStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Status inválido: " + status);
        }
    }
}
