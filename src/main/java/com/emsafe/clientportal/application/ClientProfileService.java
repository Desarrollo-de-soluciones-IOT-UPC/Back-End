package com.emsafe.clientportal.application;

import com.emsafe.clientportal.interfaces.rest.dto.ClientProfileDto;
import com.emsafe.clientportal.interfaces.rest.dto.UpdateClientProfileRequest;
import com.emsafe.iam.application.UserApplicationService;
import com.emsafe.iam.domain.repository.UserRepository;
import com.emsafe.iam.interfaces.rest.dto.ChangePasswordRequest;
import com.emsafe.shared.domain.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuenta del cliente autenticado, vista desde la app móvil.
 *
 * <p><b>La regla del BFF:</b> las <i>escrituras</i> se delegan siempre en el
 * application service del contexto dueño — aquí IAM — para que se apliquen sus
 * invariantes y se publiquen sus eventos; las <i>lecturas</i> se proyectan desde el
 * puerto de dominio. El antiguo {@code ClientService} hacía las dos cosas él mismo con
 * {@code userRepository} y {@code passwordEncoder} en la mano, duplicando lógica que ya
 * existía en IAM (la comprobación de la contraseña actual, por ejemplo).
 */
@Service
@RequiredArgsConstructor
public class ClientProfileService {

    private final UserRepository userRepository;
    private final UserApplicationService users;

    @Transactional(readOnly = true)
    public ClientProfileDto getProfile(Long clientId) {
        return userRepository.findById(clientId)
                .map(ClientProfileDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", clientId));
    }

    @Transactional
    public ClientProfileDto updateProfile(Long clientId, UpdateClientProfileRequest req) {
        users.updateOwnClientProfile(clientId, req.name(), req.phone(), req.location(),
                req.address(), req.latitude(), req.longitude());
        return getProfile(clientId);
    }

    public void changePassword(Long clientId, ChangePasswordRequest req) {
        users.changePassword(clientId, req);
    }

    public void deleteAccount(Long clientId, String password) {
        users.deleteOwnAccount(clientId, password);
    }
}
