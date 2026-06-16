package com.eduessence.auth.service;

import com.eduessence.auth.model.dto.LoginRequest;
import com.eduessence.auth.model.dto.LoginResponse;
import com.eduessence.auth.model.dto.RecuperarPasswordRequest;
import com.eduessence.auth.model.dto.RegisterRequest;
import com.eduessence.auth.model.dto.RestablecerPasswordRequest;
import com.eduessence.auth.model.dto.TokenValidacionResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request, String ip, String userAgent);

    Long register(RegisterRequest request, String ip);

    void recuperarPassword(RecuperarPasswordRequest request);

    void restablecerPassword(RestablecerPasswordRequest request);

    void logout(String token);

    TokenValidacionResponse verificarToken(String token);
}
