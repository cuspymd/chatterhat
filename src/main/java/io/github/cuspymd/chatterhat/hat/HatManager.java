package io.github.cuspymd.chatterhat.hat;

import io.github.cuspymd.chatterhat.item.ModItems;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.server.network.ServerPlayerEntity;

public class HatManager {
	public boolean isHatEquipped(ServerPlayerEntity player) {
		return player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.CHATTER_HAT);
	}
}
