package me.wonwoo;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.Collections;

import static java.util.stream.Collectors.*;

@EnableZuulProxy
@EnableBinding(Source.class)
@EnableCircuitBreaker
@EnableDiscoveryClient
@SpringBootApplication
public class ReservationClientApplication {


    @Bean
    AlwaysSampler alwaysSampler(){
        return new AlwaysSampler();
    }
    public static void main(String[] args) {
        SpringApplication.run(ReservationClientApplication.class, args);
    }
}

@RestController
@RequestMapping("/reservations")
class ReservationApiGatewayRestController {

    @Autowired
    @LoadBalanced
    private RestTemplate restTemplate;

    @Output(Source.OUTPUT)
    @Autowired
    private MessageChannel messageChannel;

    @RequestMapping(method = RequestMethod.POST)
    public void write(@RequestBody Reservation reservation) {
        this.messageChannel.send(MessageBuilder.withPayload(reservation.getReservationName()).build());
    }

    public Collection<String> getReservationNamesFallback() {
        return Collections.emptyList();
    }

    @HystrixCommand(fallbackMethod = "getReservationNamesFallback")
    @RequestMapping("/names")
    public Collection<String> getReservationNames() {
        ParameterizedTypeReference<Resources<Reservation>> ptr = new ParameterizedTypeReference<Resources<Reservation>>() {
        };

        ResponseEntity<Resources<Reservation>> responseEntity = this.restTemplate.exchange("http://reservation-service/reservations", HttpMethod.GET, null, ptr);

        return responseEntity
                .getBody()
                .getContent()
                .stream()
                .map(Reservation::getReservationName)
                .collect(toList());
    }
}


class Reservation {

    private Long id;
    private String reservationName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReservationName() {
        return reservationName;
    }

    public void setReservationName(String reservationName) {
        this.reservationName = reservationName;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Reservation{");
        sb.append("id=").append(id);
        sb.append(", reservationName='").append(reservationName).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
