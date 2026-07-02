package br.com.descontovivo.account.service;

import br.com.descontovivo.account.api.AccountDataRequestResponse;
import br.com.descontovivo.account.api.AccountDataRequestSummary;
import br.com.descontovivo.account.entity.AccountDataRequestEntity;
import br.com.descontovivo.account.entity.DataRequestStatus;
import br.com.descontovivo.account.entity.DataRequestType;
import br.com.descontovivo.account.repository.AccountDataRequestRepository;
import br.com.descontovivo.shared.security.CurrentUserProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;

import java.time.OffsetDateTime;
import java.util.List;

@ApplicationScoped
public class AccountDataRequestService {

    private static final String SUCCESS_MESSAGE =
            "Solicitação registrada. Avaliaremos o pedido conforme a legislação aplicável e as necessidades de segurança e manutenção de registros permitidos.";

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
}
