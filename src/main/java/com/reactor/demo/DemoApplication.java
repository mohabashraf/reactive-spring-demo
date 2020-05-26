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
import org.springframework.context.event.EventListener;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	RouterFunction<ServerResponse> routes(GreetingService gs){
		return route()
				.GET("greeting/name", serverRequest -> ok().body(gs.greet(), String.class))
				.build();
	}
}


@Service
class GreetingService {


	Flux<String> greetMany(){
		return Flux.fromStream(Stream.generate(() -> "Ahmed"))
				.delayElements(Duration.ofSeconds(1)).subscribeOn(Schedulers.elastic());
	}
	Mono<String> greetOnce(){
		return Mono.just("Hi there");
	}
}

	@Component
	@RequiredArgsConstructor
	@Log4j2
	class SampleDataInitializer{

		private final ReservationRepository reservationRepository;

		@EventListener(ApplicationReadyEvent.class)
		public void ready(){
			Flux<Reservation> reservation = Flux
					.just("Mohab Nazmy", "El Houssine", "Ghita Adnani", "Omar Abaza", "Umer Farouq")
					.map( name -> new Reservation(UUID.randomUUID().toString(), name))
					.flatMap(reservationRepository::save);
			reservation.subscribe();

			this.reservationRepository.deleteAll()
					.thenMany(reservation)
					.thenMany(reservationRepository.findAll())
					.subscribe(log::info);
		}


	}

	interface ReservationRepository extends ReactiveCrudRepository<Reservation, String>{
		Flux<Reservation> findByName(String name);
	}

	@Document
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	class Reservation
	{
		@Id
		private String id;
		private String name;
	}


