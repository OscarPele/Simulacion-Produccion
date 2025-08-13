package com.oscar.proyecto.ms_auth.token;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Compatibilidad (tests antiguos)
    Optional<RefreshToken> findByToken(String token);

    // Nuevos: búsquedas por HASH
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Query("select rt from RefreshToken rt join fetch rt.user where rt.tokenHash = :tokenHash")
    Optional<RefreshToken> findByTokenHashFetchUser(@Param("tokenHash") String tokenHash);

    long deleteByExpiresAtBefore(Instant instant);
    long deleteByRevokedTrueAndExpiresAtBefore(Instant instant);

    @Query("select rt.id from RefreshToken rt where rt.user.id = :userId order by rt.createdAt asc")
    List<Long> findIdsByUserOldestFirst(@Param("userId") long userId);

    // (opcional, ya existe en JpaRepository)
    void deleteAllByIdInBatch(Iterable<Long> ids);

    long countByUserId(long userId);

    List<RefreshToken> findAllByUserId(long userId);
}
