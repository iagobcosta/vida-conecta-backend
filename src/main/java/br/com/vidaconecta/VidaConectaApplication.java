package br.com.vidaconecta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class VidaConectaApplication {

	public static void main(String[] args) {
		SpringApplication.run(VidaConectaApplication.class, args);
	}

}
