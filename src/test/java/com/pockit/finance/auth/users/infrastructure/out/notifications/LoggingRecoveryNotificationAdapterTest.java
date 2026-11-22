package com.pockit.finance.auth.users.infrastructure.out.notifications;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingRecoveryNotificationAdapterTest {

    @Test
    @DisplayName("Debe registrar la notificacion sin exponer el token en texto claro en los logs")
    void shouldLogNotificationWithoutExposingRawToken() {
        Logger logger = (Logger) LoggerFactory.getLogger(LoggingRecoveryNotificationAdapter.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        try {
            LoggingRecoveryNotificationAdapter adapter = new LoggingRecoveryNotificationAdapter();
            String rawToken = "super-secret-raw-reset-token-12345";
            OffsetDateTime expiraEn = OffsetDateTime.of(2026, 11, 22, 18, 15, 0, 0, ZoneOffset.UTC);

            adapter.enviarTokenRecuperacion("camilo@pockit.com", rawToken, expiraEn);

            assertThat(listAppender.list).hasSize(1);
            String loggedMessage = listAppender.list.get(0).getFormattedMessage();
            assertThat(loggedMessage).contains("camilo@pockit.com");
            assertThat(loggedMessage).doesNotContain(rawToken);
        } finally {
            logger.detachAppender(listAppender);
        }
    }
}
