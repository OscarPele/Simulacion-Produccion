package com.oscar.proyecto.ms_auth.token;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RefreshTokenHashBackfill {

    private final RefreshTokenRepository repo;

    public RefreshTokenHashBackfill(RefreshTokenRepository repo) {
        this.repo = repo;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfill() {
        var all = repo.findAll();
        boolean changed = false;
        for (var rt : all) {
            if (rt.getTokenHash() == null && rt.getToken() != null) {
                // Reutilizamos la función del servicio (evita duplicación)
                rt.setTokenHash(RefreshTokenService.sha256Url(rt.getToken()));
                changed = true;
            }
        }
        if (changed) {
            repo.saveAll(all);
        }
    }
}
