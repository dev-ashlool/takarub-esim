package com.takarub.esim.identity.infrastructure.email;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Central service for database-driven SMTP configuration. Caches the active
 * {@link JavaMailSender} and refreshes automatically when the configuration is
 * updated or the cache expires (5-minute TTL).
 */
@Service
public class SmtpConfigService {

    private static final Logger log = LoggerFactory.getLogger(SmtpConfigService.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final SmtpConfigJpaRepository repository;
    private final SmtpConfigEncryptor encryptor;

    private volatile CachedMailSender cachedMailSender;

    public SmtpConfigService(SmtpConfigJpaRepository repository, SmtpConfigEncryptor encryptor) {
        this.repository = repository;
        this.encryptor = encryptor;
    }

    public JavaMailSender getMailSender() {
        CachedMailSender cached = this.cachedMailSender;
        if (cached != null && !cached.isStale()) {
            return cached.sender();
        }
        return loadAndCache().sender();
    }

    public String getFromEmail() {
        CachedMailSender cached = this.cachedMailSender;
        if (cached != null && !cached.isStale()) {
            return cached.fromEmail();
        }
        return loadAndCache().fromEmail();
    }

    /**
     * Returns the active SMTP configuration with a masked password (for admin display).
     */
    @Transactional(readOnly = true)
    public Optional<SmtpConfig> getActiveConfig() {
        return repository.findByActiveTrue()
                .map(entity -> new SmtpConfig(
                        entity.getId(),
                        entity.getHost(),
                        entity.getPort(),
                        entity.getUsername(),
                        "********",
                        entity.isAuthEnabled(),
                        entity.isTlsEnabled(),
                        entity.getFromEmail(),
                        entity.isActive()));
    }

    /**
     * Creates or updates the active SMTP configuration. A {@code null} password in the
     * input keeps the existing encrypted password (useful for partial updates).
     */
    @Transactional
    public SmtpConfig saveConfig(SmtpConfig config) {
        Optional<SmtpConfigEntity> existing = repository.findByActiveTrue();

        SmtpConfigEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setHost(config.host());
            entity.setPort(config.port());
            entity.setUsername(config.username());
            if (config.password() != null && !config.password().isBlank()) {
                entity.setPassword(encryptor.encrypt(config.password()));
            }
            entity.setAuthEnabled(config.authEnabled());
            entity.setTlsEnabled(config.tlsEnabled());
            entity.setFromEmail(config.fromEmail());
            entity.setActive(true);
        } else {
            if (config.password() == null || config.password().isBlank()) {
                throw new IllegalArgumentException("Password is required for initial SMTP configuration");
            }
            entity = new SmtpConfigEntity(
                    config.host(),
                    config.port(),
                    config.username(),
                    encryptor.encrypt(config.password()),
                    config.authEnabled(),
                    config.tlsEnabled(),
                    config.fromEmail(),
                    true);
        }

        SmtpConfigEntity saved = repository.save(entity);
        refreshCache();

        log.info("SMTP configuration saved: host={}, port={}, username={}, fromEmail={}",
                saved.getHost(), saved.getPort(), saved.getUsername(), saved.getFromEmail());

        return new SmtpConfig(
                saved.getId(),
                saved.getHost(),
                saved.getPort(),
                saved.getUsername(),
                "********",
                saved.isAuthEnabled(),
                saved.isTlsEnabled(),
                saved.getFromEmail(),
                saved.isActive());
    }

    public void refreshCache() {
        this.cachedMailSender = null;
        log.debug("SMTP configuration cache invalidated");
    }

    private synchronized CachedMailSender loadAndCache() {
        CachedMailSender cached = this.cachedMailSender;
        if (cached != null && !cached.isStale()) {
            return cached;
        }

        SmtpConfigEntity entity = repository.findByActiveTrue()
                .orElseThrow(NoActiveSmtpConfigException::new);

        String decryptedPassword = encryptor.decrypt(entity.getPassword());

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(entity.getHost());
        sender.setPort(entity.getPort());
        sender.setUsername(entity.getUsername());
        sender.setPassword(decryptedPassword);

        Properties props = sender.getJavaMailProperties();
        props.put("mail.smtp.auth", String.valueOf(entity.isAuthEnabled()));
        props.put("mail.smtp.starttls.enable", String.valueOf(entity.isTlsEnabled()));
        props.put("mail.transport.protocol", "smtp");

        cached = new CachedMailSender(sender, entity.getFromEmail(), Instant.now());
        this.cachedMailSender = cached;

        log.info("SMTP configuration loaded from database: host={}, port={}", entity.getHost(), entity.getPort());
        return cached;
    }

    private record CachedMailSender(JavaMailSender sender, String fromEmail, Instant loadedAt) {
        boolean isStale() {
            return Duration.between(loadedAt, Instant.now()).compareTo(CACHE_TTL) > 0;
        }
    }
}
