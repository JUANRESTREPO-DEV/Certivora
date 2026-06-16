package com.eduessence.auth.repository;

import com.eduessence.auth.model.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenAndUsadoFalse(String token);

    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.usado = true WHERE t.expira < :now AND t.usado = false")
    int marcarVencidosComoUsados(@Param("now") LocalDateTime now);
}
