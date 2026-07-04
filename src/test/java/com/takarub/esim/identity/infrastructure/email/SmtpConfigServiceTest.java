package com.takarub.esim.identity.infrastructure.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class SmtpConfigServiceTest {

    @Mock
    private SmtpConfigJpaRepository repository;

    @Mock
    private SmtpConfigEncryptor encryptor;

    private SmtpConfigService service;

    @BeforeEach
    void setUp() {
        service = new SmtpConfigService(repository, encryptor);
    }

    @Test
    void getMailSenderThrowsWhenNoActiveConfig() {
        when(repository.findByActiveTrue()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMailSender())
                .isInstanceOf(NoActiveSmtpConfigException.class);
    }

    @Test
    void getMailSenderReturnsCachedSender() {
        SmtpConfigEntity entity = new SmtpConfigEntity(
                "smtp.gmail.com", 587, "user@gmail.com", "encrypted-pw",
                true, true, "from@gmail.com", true);
        when(repository.findByActiveTrue()).thenReturn(Optional.of(entity));
        when(encryptor.decrypt("encrypted-pw")).thenReturn("plain-pw");

        JavaMailSender sender1 = service.getMailSender();
        JavaMailSender sender2 = service.getMailSender();

        assertThat(sender1).isNotNull();
        assertThat(sender1).isSameAs(sender2);
    }

    @Test
    void getFromEmailLoadsFromDatabase() {
        SmtpConfigEntity entity = new SmtpConfigEntity(
                "smtp.gmail.com", 587, "user@gmail.com", "encrypted-pw",
                true, true, "from@gmail.com", true);
        when(repository.findByActiveTrue()).thenReturn(Optional.of(entity));
        when(encryptor.decrypt("encrypted-pw")).thenReturn("plain-pw");

        assertThat(service.getFromEmail()).isEqualTo("from@gmail.com");
    }

    @Test
    void saveConfigEncryptsPasswordAndStores() {
        SmtpConfig config = new SmtpConfig(
                null, "smtp.gmail.com", 587, "user@gmail.com", "plain-password",
                true, true, "from@gmail.com", true);

        when(repository.findByActiveTrue()).thenReturn(Optional.empty());
        when(encryptor.encrypt("plain-password")).thenReturn("encrypted-password");
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SmtpConfig result = service.saveConfig(config);

        ArgumentCaptor<SmtpConfigEntity> captor = ArgumentCaptor.forClass(SmtpConfigEntity.class);
        verify(repository).save(captor.capture());
        SmtpConfigEntity saved = captor.getValue();

        assertThat(saved.getHost()).isEqualTo("smtp.gmail.com");
        assertThat(saved.getPassword()).isEqualTo("encrypted-password");
        assertThat(result.password()).isEqualTo("********");
    }

    @Test
    void saveConfigUpdatesExistingWithoutChangingPassword() {
        SmtpConfigEntity existing = new SmtpConfigEntity(
                "smtp.old.com", 587, "old@gmail.com", "old-encrypted",
                true, true, "old@gmail.com", true);
        when(repository.findByActiveTrue()).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SmtpConfig config = new SmtpConfig(
                null, "smtp.new.com", 465, "new@gmail.com", null,
                true, true, "new@gmail.com", true);

        service.saveConfig(config);

        assertThat(existing.getHost()).isEqualTo("smtp.new.com");
        assertThat(existing.getPort()).isEqualTo(465);
        assertThat(existing.getPassword()).isEqualTo("old-encrypted");
    }

    @Test
    void saveConfigRequiresPasswordForInitialSetup() {
        when(repository.findByActiveTrue()).thenReturn(Optional.empty());

        SmtpConfig config = new SmtpConfig(
                null, "smtp.gmail.com", 587, "user@gmail.com", null,
                true, true, "from@gmail.com", true);

        assertThatThrownBy(() -> service.saveConfig(config))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password is required");
    }

    @Test
    void refreshCacheInvalidatesCachedSender() {
        SmtpConfigEntity entity = new SmtpConfigEntity(
                "smtp.gmail.com", 587, "user@gmail.com", "encrypted-pw",
                true, true, "from@gmail.com", true);
        when(repository.findByActiveTrue()).thenReturn(Optional.of(entity));
        when(encryptor.decrypt("encrypted-pw")).thenReturn("plain-pw");

        JavaMailSender sender1 = service.getMailSender();
        service.refreshCache();

        SmtpConfigEntity updatedEntity = new SmtpConfigEntity(
                "smtp.new.com", 465, "new@gmail.com", "new-encrypted",
                true, true, "new@gmail.com", true);
        when(repository.findByActiveTrue()).thenReturn(Optional.of(updatedEntity));
        when(encryptor.decrypt("new-encrypted")).thenReturn("new-plain");

        JavaMailSender sender2 = service.getMailSender();

        assertThat(sender2).isNotSameAs(sender1);
    }

    @Test
    void getActiveConfigReturnsMaskedPassword() {
        SmtpConfigEntity entity = new SmtpConfigEntity(
                "smtp.gmail.com", 587, "user@gmail.com", "encrypted-pw",
                true, true, "from@gmail.com", true);
        when(repository.findByActiveTrue()).thenReturn(Optional.of(entity));

        Optional<SmtpConfig> result = service.getActiveConfig();

        assertThat(result).isPresent();
        assertThat(result.get().password()).isEqualTo("********");
        assertThat(result.get().host()).isEqualTo("smtp.gmail.com");
    }

    @Test
    void getActiveConfigReturnsEmptyWhenNoConfig() {
        when(repository.findByActiveTrue()).thenReturn(Optional.empty());

        assertThat(service.getActiveConfig()).isEmpty();
    }
}
