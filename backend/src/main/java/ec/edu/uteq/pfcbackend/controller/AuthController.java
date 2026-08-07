package ec.edu.uteq.pfcbackend.controller;

import ec.edu.uteq.pfcbackend.dto.ApiResponse;
import ec.edu.uteq.pfcbackend.dto.LoginRequest;
import ec.edu.uteq.pfcbackend.dto.LoginResponse;
import ec.edu.uteq.pfcbackend.entity.Usuario;
import ec.edu.uteq.pfcbackend.exception.InvalidCredentialsException;
import ec.edu.uteq.pfcbackend.repository.UsuarioRepository;
import ec.edu.uteq.pfcbackend.security.JwtService;
import ec.edu.uteq.pfcbackend.security.TokenBlacklistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticacion", description = "Login y logout basados en JWT via cookie HttpOnly")
public class AuthController {

    private static final String COOKIE_NAME = "access_token";

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesion")
    @SecurityRequirements
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(request.username())
                .orElseThrow(() -> new InvalidCredentialsException("Usuario o password invalidos"));

        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw new InvalidCredentialsException("Usuario o password invalidos");
        }

        String token = jwtService.generarToken(usuario.getUsername(), usuario.getRol());
        ResponseCookie cookie = construirCookie(token, Duration.ofMinutes(jwtService.getExpirationMinutes()));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(new LoginResponse(usuario.getUsername(), usuario.getRol())));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesion e invalidar el token actual")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        String token = extraerTokenDeCookie(request);

        if (token != null) {
            try {
                Claims claims = jwtService.validarYExtraerClaims(token);
                Duration tiempoRestante = Duration.between(Instant.now(), claims.getExpiration().toInstant());
                tokenBlacklistService.agregar(claims.getId(), tiempoRestante);
            } catch (JwtException ignored) {
                // token ya invalido/expirado: no hay nada que revocar
            }
        }

        ResponseCookie cookieBorrada = construirCookie("", Duration.ZERO);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookieBorrada.toString())
                .build();
    }

    private ResponseCookie construirCookie(String valor, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, valor)
                .httpOnly(true)
                // Secure=false solo para desarrollo local sin HTTPS; en produccion DEBE ser true.
                .secure(false)
                .sameSite("Strict")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private String extraerTokenDeCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
