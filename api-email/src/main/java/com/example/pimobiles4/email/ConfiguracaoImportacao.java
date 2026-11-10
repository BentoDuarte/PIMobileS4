package com.example.pimobiles4.email;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("importacao")
record ConfiguracaoImportacao(
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{32,}") String token,
        @NotBlank String projetoFirebase,
        @NotBlank @Pattern(regexp = "[^/]+") String usuarioFirebase,
        @Valid Imap imap
) {
    record Imap(
            @NotBlank String host,
            @Min(1) @Max(65535) int porta,
            @NotBlank String usuario,
            @NotBlank String senha,
            @NotBlank String pasta,
            String proxyHost,
            @Min(1) @Max(65535) int proxyPorta,
            @NotBlank @Pattern(regexp = "socks5|http") String proxyTipo
    ) {}
}
