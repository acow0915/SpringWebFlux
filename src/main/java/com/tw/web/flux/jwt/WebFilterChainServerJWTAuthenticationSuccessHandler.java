package com.tw.web.flux.jwt;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import reactor.core.publisher.Mono;

public class WebFilterChainServerJWTAuthenticationSuccessHandler 
			implements ServerAuthenticationSuccessHandler{
	
	final static long EXPIRATIONTIME = 15*60*1000; 		// 15 minutes
	public final static String SECRET = "ghwio#$%^&gj2otu29t%^&*230*()910491g";			// private key, better read it from an external file
	
	final public String TOKEN_PREFIX = "Bearer";			// the prefix of the token in the http header
	final public String HEADER_STRING = "Authorization";	// the http header containing the prexif + the token

	@Override
	public Mono<Void> onAuthenticationSuccess(WebFilterExchange webFilterExchange, Authentication authentication) {
		ServerWebExchange exchange = webFilterExchange.getExchange();
		exchange.getResponse().getHeaders()
		.add(HttpHeaders.AUTHORIZATION, getHttpAuthHeaderValue(authentication));
		return webFilterExchange.getChain().filter(exchange);
	}

	private String getHttpAuthHeaderValue(Authentication authentication) {
		
		String JWT = Jwts.builder()
				 .setId(UUID.randomUUID().toString())
				 .setSubject(authentication.getName())
				 .setIssuedAt(new Date())
				 .setExpiration(new Date(System.currentTimeMillis() + EXPIRATIONTIME))
				 .signWith(SignatureAlgorithm.HS512, SECRET)
				 .claim("auths", authentication.getAuthorities())
				 .compact();
		
		return JWT;
	}
}
