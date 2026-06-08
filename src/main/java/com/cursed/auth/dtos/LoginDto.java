package com.cursed.auth.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginDto {
    String email;
    String password;

    /**
     * Optional. Present when the login is part of an OAuth authorization flow: it is the
     * login_id minted at /authorize and forwarded to the React login app, which sends it
     * back here. When set, a successful login issues an authorization code and the response
     * carries the redirect URL back to the client.
     */
    String loginId;
}
