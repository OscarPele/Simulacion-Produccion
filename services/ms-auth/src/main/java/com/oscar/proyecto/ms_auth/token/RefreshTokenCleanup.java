package com.oscar.proyecto.ms_auth.token;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RefreshTokenCleanup {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanup.class);

    private final RefreshTokenRepository repo;

    public RefreshTokenCleanup(RefreshTokenRepository repo) {
        this.repo = repo;
    }

    /** Limpieza automática cada hora. */
    @Transactional
    @Scheduled(cron = "0 0 * * * *")
    public void clean() {
        Instant now = Instant.now();

        long removedExpired = repo.deleteByExpiresAtBefore(now);
        long removedRevoked = repo.deleteByRevokedTrueAndExpiresAtBefore(now);

        long total = removedExpired + removedRevoked;
        if (total > 0) {
            log.info("RefreshTokenCleanup: eliminados {} tokens (caducados={}, revocados_caducados={})",
                    total, removedExpired, removedRevoked);
        } else {
            log.debug("RefreshTokenCleanup: no había tokens para eliminar.");
        }
    }
}
