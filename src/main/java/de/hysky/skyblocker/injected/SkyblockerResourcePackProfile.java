package de.hysky.skyblocker.injected;

import org.jetbrains.annotations.Nullable;

import de.hysky.skyblocker.stp.SkyblockerRPMetadata;

public interface SkyblockerResourcePackProfile {

	default void setSkyblockerMetadata(SkyblockerRPMetadata metadata) {
	}

	@Nullable
	default SkyblockerRPMetadata getSkyblockerMetadata() {
		return null;
	}
}
