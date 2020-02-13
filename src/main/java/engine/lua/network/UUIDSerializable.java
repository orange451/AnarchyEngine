package engine.lua.network;

import java.util.UUID;

public class UUIDSerializable {
	public long leastBits;
	public long mostBits;
	
	public UUIDSerializable() {
		// For Kryonet serialization
	}
	
	public UUIDSerializable(UUID uuid) {
		this.leastBits = uuid.getLeastSignificantBits();
		this.mostBits = uuid.getMostSignificantBits();
	}
	
	public UUID getUUID() {
		return new UUID(mostBits, leastBits);
	}
}
