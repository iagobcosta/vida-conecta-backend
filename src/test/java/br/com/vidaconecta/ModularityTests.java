package br.com.vidaconecta;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

	@Test
	void shouldVerifyModularStructure() {
		ApplicationModules modules = ApplicationModules.of(VidaConectaApplication.class);
		modules.verify();
	}
}
