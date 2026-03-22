package io.github.cuspymd.chatterhat.item;

import io.github.cuspymd.chatterhat.ChatterHatMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModItems {
	public static final Item CHATTER_HAT = Registry.register(
		Registries.ITEM,
		Identifier.of(ChatterHatMod.MOD_ID, "chatter_hat"),
		new ChatterHatItem(new Item.Settings().maxCount(1).equippable(EquipmentSlot.HEAD))
	);

	private ModItems() {
	}

	public static void register() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(CHATTER_HAT));
	}
}
