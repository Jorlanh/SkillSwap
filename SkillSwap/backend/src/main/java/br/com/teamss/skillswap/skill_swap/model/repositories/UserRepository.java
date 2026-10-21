package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Encontra um usuário pelo seu endereço de e-mail.
     * <strong>Este é o método essencial para o BanCheckFilter.</strong>
     * Ele vincula o usuário autenticado via Auth0 (pela claim 'email' do JWT)
     * ao registro do usuário local no seu banco de dados (para checar o ban).
     */
    Optional<User> findByEmail(String email);

    /**
     * Encontra um usuário pelo seu nome de usuário (username).
     * Continua sendo muito importante para endpoints públicos de perfil
     * (ex: /api/users/{username}) e para buscas.
     */
    Optional<User> findByUsername(String username);

    // ---------------------------------------------------------------------------------
    // Os métodos abaixo não são mais necessários para o fluxo de login/registro
    // com o Auth0, mas podem ser mantidos se outra parte do seu sistema os utilizar.
    // ---------------------------------------------------------------------------------

    /**
     * @deprecated (Para registro) O Auth0 agora gerencia a verificação de e-mail.
     */
    boolean existsByEmail(String email);

    /**
     * @deprecated (Para registro) O Auth0 agora gerencia a disponibilidade de username
     * (ou você pode tratar isso na lógica de provisionamento JIT no BanCheckFilter).
     */
    boolean existsByUsername(String username);

    /**
     * @deprecated (Para verificação) O Auth0 agora gerencia códigos de verificação.
     */
    Optional<User> findByVerificationCode(String verificationCode);
    List<User> findTop20ByOrderByKarmaPointsDesc();
}
