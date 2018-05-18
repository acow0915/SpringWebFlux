package com.tw.web.flux.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.tw.web.flux.entity.User;

import reactor.core.publisher.Mono;

@Component
public class UserHandler {
	
	private static List<User> list = new ArrayList<>();
	
	static {
		list.add(new User("A", 1));
		list.add(new User("B", 2));
		list.add(new User("C", 3));
	}
	
	public Optional<User> findByName(final String name){
		return list.stream().filter(u -> u.getName().equals(name)).findAny();
	}

	public Mono<ServerResponse> listUsers(ServerRequest request) {
		return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
				.body(Mono.just(list), List.class);
	}
	
	public Mono<ServerResponse> getUserByName(ServerRequest request) {
		return findByName(request.queryParam("name").orElse(""))
				.map(u -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
						  .body(Mono.just(u), User.class))
				.orElseGet(() -> ServerResponse.badRequest().build());
	}
}
