package com.tw.web.flux.jwt;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.HttpBasicServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authentication.ServerAuthenticationEntryPointFailureHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationFailureHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.WebFilterChainServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.security.web.server.context.ServerSecurityContextRepository;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.mysql.cj.util.StringUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import reactor.core.publisher.Mono;

public class JWTAuthorizationWebFilter implements WebFilter {
	
	private static final String BEARER = "Bearer";
	private static final String REFRESH_BEARER = "Ref_Bearer";
    private static final Predicate<String> matchBearerLength = authValue -> (authValue.length() > BEARER.length());
    
    private ServerSecurityContextRepository securityContextRepository = NoOpServerSecurityContextRepository.getInstance();
    private ServerAuthenticationSuccessHandler authenticationSuccessHandler = new WebFilterChainServerAuthenticationSuccessHandler();
    
    private ReactiveAuthenticationManager authenticationManager = new JwtReactiveAuthenticationManager();
    
    private ServerAuthenticationFailureHandler authenticationFailureHandler = new ServerAuthenticationEntryPointFailureHandler(new HttpBasicServerAuthenticationEntryPoint());

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		String jwtToken = exchange.getRequest().getHeaders().getFirst(BEARER);
		System.out.println("jwtToken:" + jwtToken);
		if(StringUtils.isNullOrEmpty(jwtToken)) {
			String authStr = exchange.getResponse().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
			System.out.println("first:" + authStr);
			return ServerWebExchangeMatchers.pathMatchers("/**").matches(exchange)
					.filter(matchResult -> matchResult.isMatch())
					.flatMap(matchResult -> 
						Mono.justOrEmpty(exchange.getResponse().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
						.filter(s -> !StringUtils.isNullOrEmpty(s))
						.filter(matchBearerLength)
						.filter(token -> !token.isEmpty())
						.flatMap(token -> {
							System.out.println("second:" + token);
							Claims claims = null;
							try {
								claims = Jwts.parser().setSigningKey(WebFilterChainServerJWTAuthenticationSuccessHandler.SECRET).parseClaimsJws(token).getBody();
							} catch (io.jsonwebtoken.ExpiredJwtException e) {
								return Mono.empty();
							}
							System.out.println(claims.get("auths"));
							@SuppressWarnings("unchecked")
							List<Map<String, Object>> authdatas = (List<Map<String, Object>>) claims.get("auths");getClass();
							List<SimpleGrantedAuthority> auths = 
									authdatas.stream().map(m -> new SimpleGrantedAuthority((String)m.get("authority")))
									.collect(Collectors.toList());
							return Mono.just(new UsernamePasswordAuthenticationToken(claims.getSubject(), null, auths));
						})
					)
					.switchIfEmpty(chain.filter(exchange).then(Mono.empty()))
					.flatMap(token -> authenticate(exchange, chain, token));
		} else {
			return ServerWebExchangeMatchers.pathMatchers("/**").matches(exchange)
			.filter(matchResult -> matchResult.isMatch())
			.flatMap(matchResult -> Mono.justOrEmpty(jwtToken))
			.flatMap(token -> {
				Claims claims = null;
				try {
					claims = Jwts.parser().setSigningKey(WebFilterChainServerJWTAuthenticationSuccessHandler.SECRET).parseClaimsJws(token).getBody();
				} catch (io.jsonwebtoken.ExpiredJwtException e) {
					return Mono.empty();
				}
				System.out.println(claims.get("auths"));
				@SuppressWarnings("unchecked")
				List<Map<String, Object>> authdatas = (List<Map<String, Object>>) claims.get("auths");getClass();
				List<SimpleGrantedAuthority> auths = 
						authdatas.stream().map(m -> new SimpleGrantedAuthority((String)m.get("authority")))
						.collect(Collectors.toList());
				return Mono.just(new UsernamePasswordAuthenticationToken(claims.getSubject(), null, auths));
			})
			.switchIfEmpty(chain.filter(exchange).then(Mono.empty()))
			.flatMap(token -> authenticate(exchange, chain, token));
		}
	}
	
	private Mono<Void> authenticate(ServerWebExchange exchange,
		WebFilterChain chain, Authentication token) {
		WebFilterExchange webFilterExchange = new WebFilterExchange(exchange, chain);
		return this.authenticationManager.authenticate(token)
			.flatMap(authentication -> onAuthenticationSuccess(authentication, webFilterExchange))
			.onErrorResume(AuthenticationException.class, e -> this.authenticationFailureHandler
					.onAuthenticationFailure(webFilterExchange, e));
	}

	private Mono<Void> onAuthenticationSuccess(Authentication authentication, WebFilterExchange webFilterExchange) {
		ServerWebExchange exchange = webFilterExchange.getExchange();
		SecurityContextImpl securityContext = new SecurityContextImpl();
		securityContext.setAuthentication(authentication);
		return this.securityContextRepository.save(exchange, securityContext)
			.then(this.authenticationSuccessHandler
				.onAuthenticationSuccess(webFilterExchange, authentication))
			.subscriberContext(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(securityContext)));
	}
	
//	public static void main(String[] args) {
//		String token = "eyJhbGciOiJIUzUxMiJ9.eyJqdGkiOiJmMjM5NTk4ZS0wYzAyLTQ3MzktOWMwNy00NGY4M2NmYjQ2ZWQiLCJzdWIiOiJBIiwiaWF0IjoxNTI2ODk3MTkyLCJleHAiOjE1MjY4OTgwOTIsImF1dGhzIjpbeyJhdXRob3JpdHkiOiJST0xFX1VTRVIifV19.ZI8zoI4GT5zYnqtFqHWa9fzdSMdyQpVVn4YZr4y_Id7SjOxFxnFqiHAnq4Ati8fyCedN80IkNDSIPfDln4LYUA";
//		Jwt jwt = Jwts.parser().setSigningKey(WebFilterChainServerJWTAuthenticationSuccessHandler.SECRET).parse(token);
//		System.out.println(jwt.getHeader());
//		System.out.println(jwt.getBody());
//		
//		Claims claims = Jwts.parser().setSigningKey(WebFilterChainServerJWTAuthenticationSuccessHandler.SECRET).parseClaimsJws(token).getBody();
//		System.out.println(claims);
//	}

}
