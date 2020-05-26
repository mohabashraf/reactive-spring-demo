package com.reactor.demo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;
import static org.springframework.web.reactive.function.server.ServerResponse.status;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    RouterFunction<ServerResponse> routes(GreetingService gs) {
        return route()
                .GET("greetingOnce/{name}", serverRequest -> ok().body(gs.greetOnce(serverRequest.pathVariable("name")),String.class))
                .GET("greeting/name", serverRequest -> handleGreeting(gs))
                .build();
    }

    private Mono<ServerResponse> handleGreeting(GreetingService gs) {
        return ok().contentType(MediaType.TEXT_EVENT_STREAM).body(gs.greetMany(), String.class);
    }
}

@Configuration
@Log4j2
class GreetingWebSocketConfiguration {
    @Bean
    SimpleUrlHandlerMapping simpleUrlHandlerMapping(WebSocketHandler webSocketHandler) {
        return new SimpleUrlHandlerMapping(Map.of("ws/greeting", webSocketHandler), 10);
    }

    @Bean
    WebSocketHandler webSocketHandler(GreetingService greetingService) {
        return webSocketSession -> {

            var receive = webSocketSession.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .flatMap(greetingService::greet)
                    .map(webSocketSession::textMessage)
                    .doOnEach(webSocketMessageSignal -> log.info(webSocketMessageSignal.getType()))
                    .doFinally(signal -> log.info("finally"+ signal.toString()));
            return webSocketSession.send(receive);

        };
    }

    @Bean
    WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}

@Service
class GreetingService {


    Flux<String> greetMany() {
        return Flux.fromStream(Stream.generate(() -> "Ahmed"))
                .delayElements(Duration.ofSeconds(1)).subscribeOn(Schedulers.elastic());
    }

    Mono<String> greetOnce(String name) {
        return Mono.just("Hi there "+ name);
    }

    Flux<String> greet(String name) {
        return Flux.fromStream(
                Stream.generate(() -> "The name = " + name)
        ).delayElements(Duration.ofSeconds(10));
    }
}

@Component
@RequiredArgsConstructor
@Log4j2
class SampleDataInitializer {

    private final ReservationRepository reservationRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void ready() {
        Flux<Reservation> reservation = Flux
                .just("Mohab Nazmy", "El Houssine", "Ghita Adnani", "Omar Abaza", "Umer Farouq")
                .map(name -> new Reservation(UUID.randomUUID().toString(), name))
                .flatMap(reservationRepository::save);
        reservation.subscribe();

        this.reservationRepository.deleteAll()
                .thenMany(reservation)
                .thenMany(reservationRepository.findAll())
                .subscribe(log::info);
    }


}

interface ReservationRepository extends ReactiveCrudRepository<Reservation, String> {
    Flux<Reservation> findByName(String name);
}

@Document
@Data
@AllArgsConstructor
@NoArgsConstructor
class Reservation {
    @Id
    private String id;
    private String name;
}


