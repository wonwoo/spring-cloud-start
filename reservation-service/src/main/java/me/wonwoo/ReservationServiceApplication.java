package me.wonwoo;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.integration.annotation.MessageEndpoint;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Arrays;
import java.util.Collection;


@EnableBinding(Sink.class)
@EnableDiscoveryClient
@SpringBootApplication
public class ReservationServiceApplication {


	@Bean
	AlwaysSampler alwaysSampler(){
		return new AlwaysSampler();
	}

	@Bean
	CommandLineRunner runner (ReservationRepository reservationRepository){
		return args -> {
			Arrays.asList("wonwoo, Dr. Rod, Syter, Juergen, ALL THE COMMUNITY, Josh".split(","))
					.forEach(x -> reservationRepository.save(new Reservation(x)));
			reservationRepository.findAll().forEach(System.out::println);
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(ReservationServiceApplication.class, args);
	}
}

@MessageEndpoint
class MessageReservationReveiver{

	@ServiceActivator(inputChannel = Sink.INPUT)
	public void acceptReservation(String rn){
		reservationRepository.save(new Reservation(rn));
	}
	@Autowired
	private ReservationRepository reservationRepository;
}

@RefreshScope
@RestController
class MessageRestController {

	@Value("${message}")
	private String message;

	@RequestMapping("/message")
	String message(){
		return this.message;
	}


}

@RepositoryRestResource
interface  ReservationRepository extends JpaRepository<Reservation,Long>{

	@RestResource(path = "by-name")
	Collection<Reservation> findByReservationName(String rn);
}

@Entity
@Data
@NoArgsConstructor
class Reservation{

	@Id
	@GeneratedValue
	private Long id;
	private String reservationName;

	public Reservation(String reservationName){
		this.reservationName = reservationName;
	}

}
