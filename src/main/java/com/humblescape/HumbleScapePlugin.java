
package com.humblescape;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import javax.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GameStateChanged;

import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@PluginDescriptor(
		name = "HumbleScape",
		description = "Removes cosmetic upgrades from other players' equipment"
)
public class HumbleScapePlugin extends Plugin
{
	@Inject
	private Client client;

	private static final Map<Integer, Integer> REPLACEMENTS =
			new HashMap<>();

	// Only compositions we have actually modified.
	private final Map<PlayerComposition, Map<Integer, Integer>>
			originalEquipment = new IdentityHashMap<>();

	static
	{
		// Infernal cape -> Fire cape
		REPLACEMENTS.put(ItemID.INFERNAL_CAPE, ItemID.FIRE_CAPE);
		REPLACEMENTS.put(ItemID.INFERNAL_CAPE_21297, ItemID.FIRE_CAPE);
		REPLACEMENTS.put(ItemID.INFERNAL_CAPE_23622, ItemID.FIRE_CAPE);
		REPLACEMENTS.put(ItemID.INFERNAL_CAPE_L, ItemID.FIRE_CAPE);

		// Dizana's quiver -> Ava's assembler
		REPLACEMENTS.put(ItemID.DIZANAS_QUIVER_UNCHARGED, ItemID.AVAS_ASSEMBLER);
		REPLACEMENTS.put(ItemID.DIZANAS_QUIVER_L_UNCHARGED, ItemID.AVAS_ASSEMBLER);
		REPLACEMENTS.put(ItemID.DIZANAS_QUIVER, ItemID.AVAS_ASSEMBLER);
		REPLACEMENTS.put(ItemID.DIZANAS_QUIVER_L, ItemID.AVAS_ASSEMBLER);
		REPLACEMENTS.put(ItemID.BLESSED_DIZANAS_QUIVER, ItemID.AVAS_ASSEMBLER);
		REPLACEMENTS.put(ItemID.BLESSED_DIZANAS_QUIVER_L, ItemID.AVAS_ASSEMBLER);

		// Zuk helm -> Slayer helmet
		REPLACEMENTS.put(ItemID.TZKAL_SLAYER_HELMET, ItemID.SLAYER_HELMET);
		REPLACEMENTS.put(ItemID.TZTOK_SLAYER_HELMET_I, ItemID.SLAYER_HELMET);
		REPLACEMENTS.put(ItemID.TZTOK_SLAYER_HELMET_I_25902, ItemID.SLAYER_HELMET);
		REPLACEMENTS.put(ItemID.VAMPYRIC_SLAYER_HELMET, ItemID.SLAYER_HELMET);
		REPLACEMENTS.put(ItemID.VAMPYRIC_SLAYER_HELMET_I, ItemID.SLAYER_HELMET);
		REPLACEMENTS.put(ItemID.VAMPYRIC_SLAYER_HELMET_I_25908, ItemID.SLAYER_HELMET);
		REPLACEMENTS.put(ItemID.TZKAL_SLAYER_HELMET, ItemID.SLAYER_HELMET);
		REPLACEMENTS.put(ItemID.TZKAL_SLAYER_HELMET_I, ItemID.SLAYER_HELMET);
		REPLACEMENTS.put(ItemID.TZKAL_SLAYER_HELMET_I_25914, ItemID.SLAYER_HELMET);

		// Radiant oathplate -> Regular oathplate
		REPLACEMENTS.put(ItemID.RADIANT_OATHPLATE_HELM, ItemID.OATHPLATE_HELM);
		REPLACEMENTS.put(ItemID.RADIANT_OATHPLATE_CHEST, ItemID.OATHPLATE_CHEST);
		REPLACEMENTS.put(ItemID.RADIANT_OATHPLATE_LEGS, ItemID.OATHPLATE_LEGS );

		// Sanguine Torva -> Regular Torva
		REPLACEMENTS.put(ItemID.SANGUINE_TORVA_FULL_HELM, ItemID.TORVA_FULL_HELM);
		REPLACEMENTS.put(ItemID.SANGUINE_TORVA_PLATEBODY, ItemID.TORVA_PLATEBODY);
		REPLACEMENTS.put(ItemID.SANGUINE_TORVA_PLATELEGS, ItemID.TORVA_PLATELEGS);

		// Fang (or) -> Regular fang
		REPLACEMENTS.put(ItemID.OSMUMTENS_FANG_OR, ItemID.OSMUMTENS_FANG);

		// Radiant oathplate -> Regular oathplate
		REPLACEMENTS.put(ItemID.GHOMMALS_AVERNIC_DEFENDER_5, ItemID.DRAGON_DEFENDER);
		REPLACEMENTS.put(ItemID.GHOMMALS_AVERNIC_DEFENDER_5_L, ItemID.DRAGON_DEFENDER);
		REPLACEMENTS.put(ItemID.GHOMMALS_AVERNIC_DEFENDER_6, ItemID.DRAGON_DEFENDER);
		REPLACEMENTS.put(ItemID.GHOMMALS_AVERNIC_DEFENDER_6_L, ItemID.DRAGON_DEFENDER);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		// Identity-based set of currently loaded compositions.
		Set<PlayerComposition> active =
				Collections.newSetFromMap(new IdentityHashMap<>());

		for (Player player : client.getPlayers())
		{
			if (player == null || player == client.getLocalPlayer())
			{
				continue;
			}

			PlayerComposition composition =
					player.getPlayerComposition();

			if (composition == null)
			{
				continue;
			}

			active.add(composition);
			applyTransmogs(composition);
		}

		// Forget compositions that are no longer loaded.
		// They are no longer being rendered by this client.
		Iterator<PlayerComposition> iterator =
				originalEquipment.keySet().iterator();

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

		Map<Integer, Integer> saved =
				originalEquipment.get(composition);

		boolean changed = false;

		for (int slot = 0; slot < equipment.length; slot++)
		{
			int equippedId = equipment[slot];

			// If we previously modified this slot, check whether
			// the game has updated it since our modification.
			if (saved != null && saved.containsKey(slot))
			{
				int originalId = saved.get(slot);
				int originalItem =
						originalId - PlayerComposition.ITEM_OFFSET;

				Integer replacement =
						REPLACEMENTS.get(originalItem);

				int expectedId = replacement == null
						? -1
						: replacement + PlayerComposition.ITEM_OFFSET;

				if (equippedId != expectedId)
				{
					// Server changed this equipment slot.
					saved.remove(slot);
				}
			}

			if (equippedId < PlayerComposition.ITEM_OFFSET)
			{
				continue;
			}

			int itemId =
					equippedId - PlayerComposition.ITEM_OFFSET;

			Integer replacement = REPLACEMENTS.get(itemId);

			if (replacement == null)
			{
				continue;
			}

			if (saved == null)
			{
				saved = new HashMap<>();
				originalEquipment.put(composition, saved);
			}

			saved.put(slot, equippedId);

			equipment[slot] =
					replacement + PlayerComposition.ITEM_OFFSET;

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
		for (Map.Entry<PlayerComposition, Map<Integer, Integer>> entry :
				originalEquipment.entrySet())
		{
			PlayerComposition composition = entry.getKey();
			int[] equipment = composition.getEquipmentIds();

			if (equipment == null)
			{
				continue;
			}

			boolean changed = false;

			for (Map.Entry<Integer, Integer> slotEntry :
					entry.getValue().entrySet())
			{
				int slot = slotEntry.getKey();
				int originalId = slotEntry.getValue();

				if (slot < 0 || slot >= equipment.length)
				{
					continue;
				}

				Integer replacement = REPLACEMENTS.get(
						originalId - PlayerComposition.ITEM_OFFSET
				);

				if (replacement == null)
				{
					continue;
				}

				int expectedId =
						replacement + PlayerComposition.ITEM_OFFSET;

				// Don't overwrite equipment that has changed.
				if (equipment[slot] == expectedId)
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
