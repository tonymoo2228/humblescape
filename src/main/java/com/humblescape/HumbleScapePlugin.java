
package com.humblescape;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
		name = "HumbleScape",
		description = "Humble your friends by changing their gear"
)
public class HumbleScapePlugin extends Plugin
{
	private static final Map<Integer, Integer> REPLACEMENTS = new HashMap<>();

	static
	{
		// Infernal cape
		REPLACEMENTS.put(ItemID.INFERNAL_CAPE, ItemID.FIRE_CAPE);
		REPLACEMENTS.put(ItemID.INFERNAL_CAPE_21297, ItemID.FIRE_CAPE);
		REPLACEMENTS.put(ItemID.INFERNAL_CAPE_23622, ItemID.FIRE_CAPE);
		REPLACEMENTS.put(ItemID.INFERNAL_CAPE_L, ItemID.FIRE_CAPE);

		// Dizana's quiver
		REPLACEMENTS.put(ItemID.DIZANAS_QUIVER_UNCHARGED, ItemID.AVAS_ASSEMBLER);
		REPLACEMENTS.put(ItemID.DIZANAS_QUIVER_L_UNCHARGED, ItemID.AVAS_ASSEMBLER);
		REPLACEMENTS.put(ItemID.DIZANAS_QUIVER, ItemID.AVAS_ASSEMBLER);
		REPLACEMENTS.put(ItemID.DIZANAS_QUIVER_L, ItemID.AVAS_ASSEMBLER);
		REPLACEMENTS.put(ItemID.BLESSED_DIZANAS_QUIVER, ItemID.AVAS_ASSEMBLER);
		REPLACEMENTS.put(ItemID.BLESSED_DIZANAS_QUIVER_L, ItemID.AVAS_ASSEMBLER);

		// Slayer helmets
		REPLACEMENTS.put(ItemID.TZTOK_SLAYER_HELMET, ItemID.SLAYER_HELMET);
		REPLACEMENTS.put(ItemID.TZTOK_SLAYER_HELMET_I, ItemID.SLAYER_HELMET);
		REPLACEMENTS.put(ItemID.TZTOK_SLAYER_HELMET_I_25902, ItemID.SLAYER_HELMET);
		REPLACEMENTS.put(ItemID.VAMPYRIC_SLAYER_HELMET, ItemID.SLAYER_HELMET);
		REPLACEMENTS.put(ItemID.VAMPYRIC_SLAYER_HELMET_I, ItemID.SLAYER_HELMET);
		REPLACEMENTS.put(ItemID.VAMPYRIC_SLAYER_HELMET_I_25908, ItemID.SLAYER_HELMET);
		REPLACEMENTS.put(ItemID.TZKAL_SLAYER_HELMET, ItemID.SLAYER_HELMET);
		REPLACEMENTS.put(ItemID.TZKAL_SLAYER_HELMET_I, ItemID.SLAYER_HELMET);
		REPLACEMENTS.put(ItemID.TZKAL_SLAYER_HELMET_I_25914, ItemID.SLAYER_HELMET);

		// Radiant oathplate
		REPLACEMENTS.put(ItemID.RADIANT_OATHPLATE_HELM, ItemID.OATHPLATE_HELM);
		REPLACEMENTS.put(ItemID.RADIANT_OATHPLATE_CHEST, ItemID.OATHPLATE_CHEST);
		REPLACEMENTS.put(ItemID.RADIANT_OATHPLATE_LEGS, ItemID.OATHPLATE_LEGS);

		// Sanguine Torva
		REPLACEMENTS.put(ItemID.SANGUINE_TORVA_FULL_HELM, ItemID.TORVA_FULL_HELM);
		REPLACEMENTS.put(ItemID.SANGUINE_TORVA_PLATEBODY, ItemID.TORVA_PLATEBODY);
		REPLACEMENTS.put(ItemID.SANGUINE_TORVA_PLATELEGS, ItemID.TORVA_PLATELEGS);

		// Fang ornament kit
		REPLACEMENTS.put(ItemID.OSMUMTENS_FANG_OR, ItemID.OSMUMTENS_FANG);

		// Ghommal's Avernic defender
		REPLACEMENTS.put(ItemID.GHOMMALS_AVERNIC_DEFENDER_5, ItemID.DRAGON_DEFENDER);
		REPLACEMENTS.put(ItemID.GHOMMALS_AVERNIC_DEFENDER_5_L, ItemID.DRAGON_DEFENDER);
		REPLACEMENTS.put(ItemID.GHOMMALS_AVERNIC_DEFENDER_6, ItemID.DRAGON_DEFENDER);
		REPLACEMENTS.put(ItemID.GHOMMALS_AVERNIC_DEFENDER_6_L, ItemID.DRAGON_DEFENDER);
	}

	@Inject
	private Client client;

	private final Map<PlayerComposition, Map<Integer, Integer>> originalEquipment =
			new IdentityHashMap<>();

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		Set<PlayerComposition> active = Collections.newSetFromMap(new IdentityHashMap<>());

		for (Player player : client.getPlayers())
		{
			if (player == null || player == client.getLocalPlayer())
			{
				continue;
			}

			PlayerComposition composition = player.getPlayerComposition();
			if (composition == null)
			{
				continue;
			}

			active.add(composition);
			applyTransmogs(composition);
		}

		Iterator<PlayerComposition> iterator = originalEquipment.keySet().iterator();
		while (iterator.hasNext())
		{
			if (!active.contains(iterator.next()))
			{
				iterator.remove();
			}
		}
	}

	private void applyTransmogs(PlayerComposition composition)
	{
		int[] equipment = composition.getEquipmentIds();
		if (equipment == null)
		{
			return;
		}

		Map<Integer, Integer> saved = originalEquipment.get(composition);
		boolean changed = false;

		for (int slot = 0; slot < equipment.length; slot++)
		{
			int equippedId = equipment[slot];

			if (saved != null && saved.containsKey(slot))
			{
				int expectedId = replacementEquipmentId(saved.get(slot));
				if (equippedId != expectedId)
				{
					saved.remove(slot);
				}
			}

			if (equippedId < PlayerComposition.ITEM_OFFSET)
			{
				continue;
			}

			int replacementId = replacementEquipmentId(equippedId);
			if (replacementId == -1)
			{
				continue;
			}

			if (saved == null)
			{
				saved = new HashMap<>();
				originalEquipment.put(composition, saved);
			}

			saved.put(slot, equippedId);
			equipment[slot] = replacementId;
			changed = true;
		}

		if (saved != null && saved.isEmpty())
		{
			originalEquipment.remove(composition);
		}

		if (changed)
		{
			composition.setHash();
		}
	}

	private static int replacementEquipmentId(int equipmentId)
	{
		if (equipmentId < PlayerComposition.ITEM_OFFSET)
		{
			return -1;
		}

		int itemId = equipmentId - PlayerComposition.ITEM_OFFSET;
		Integer replacement = REPLACEMENTS.get(itemId);
		return replacement == null ? -1 : replacement + PlayerComposition.ITEM_OFFSET;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN ||
				event.getGameState() == GameState.HOPPING)
		{
			originalEquipment.clear();
		}
	}

	@Override
	protected void shutDown()
	{
		for (Map.Entry<PlayerComposition, Map<Integer, Integer>> entry : originalEquipment.entrySet())
		{
			PlayerComposition composition = entry.getKey();
			int[] equipment = composition.getEquipmentIds();

			if (equipment == null)
			{
				continue;
			}

			boolean changed = false;

			for (Map.Entry<Integer, Integer> slotEntry : entry.getValue().entrySet())
			{
				int slot = slotEntry.getKey();
				int originalId = slotEntry.getValue();

				if (slot < 0 || slot >= equipment.length)
				{
					continue;
				}

				if (equipment[slot] == replacementEquipmentId(originalId))
				{
					equipment[slot] = originalId;
					changed = true;
				}
			}

			if (changed)
			{
				composition.setHash();
			}
		}

		originalEquipment.clear();
	}
}
