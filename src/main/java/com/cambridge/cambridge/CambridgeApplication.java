package com.cambridge.cambridge;

import com.cambridge.cambridge.services.MapParser;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CambridgeApplication
{

	public static void main(String[] args)
	{
		SpringApplication.run(CambridgeApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(MapParser mapParser) {
		return new CommandLineRunner() {
			@Override
			public void run(String... args) {
				try {
					String pbfFile = "src/main/street_data/cambridgeshire-251109.osm.pbf";
					mapParser.parse(pbfFile);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
	}

}
