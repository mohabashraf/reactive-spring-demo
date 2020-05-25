package com.reactor.demo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.UUID;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
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


