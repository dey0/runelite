/*
 * Copyright (c) 2017, Tyler <http://github.com/tylerthardy>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.runepouch;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.ItemID;
import net.runelite.api.Point;
import net.runelite.api.Query;
import net.runelite.api.Varbits;
import net.runelite.api.queries.InventoryItemQuery;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.RuneLite;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayRenderer;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.tooltips.Tooltip;
import net.runelite.client.ui.overlay.tooltips.TooltipPriority;
import net.runelite.client.ui.overlay.tooltips.TooltipRenderer;

public class RunepouchOverlay extends Overlay
{
	private static final Varbits[] AMOUNT_VARBITS =
	{
		Varbits.RUNE_POUCH_AMOUNT1, Varbits.RUNE_POUCH_AMOUNT2, Varbits.RUNE_POUCH_AMOUNT3
	};
	private static final Varbits[] RUNE_VARBITS =
	{
		Varbits.RUNE_POUCH_RUNE1, Varbits.RUNE_POUCH_RUNE2, Varbits.RUNE_POUCH_RUNE3
	};

	private final RuneImageCache runeImageCache = new RuneImageCache();

	private final RuneLite runelite;
	private final Client client;
	private final RunepouchConfig config;
	private final TooltipRenderer tooltipRenderer;

	@Inject
	RunepouchOverlay(RuneLite runelite, RunepouchConfig config, OverlayRenderer overlayRenderer)
	{
		super(OverlayPosition.DYNAMIC);
		this.runelite = runelite;
		this.client = runelite.getClient();
		this.config = config;
		this.tooltipRenderer = overlayRenderer.getTooltipRenderer();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.enabled()
			|| client.getGameState() != GameState.LOGGED_IN
			|| client.getWidget(WidgetInfo.LOGIN_CLICK_TO_PLAY_SCREEN) != null)
		{
			return null;
		}

		Query query = new InventoryItemQuery().idEquals(ItemID.RUNE_POUCH);
		WidgetItem[] items = runelite.runQuery(query);
		if (items.length == 0)
		{
			return null;
		}

		WidgetItem runePouch = items[0];
		Point location = runePouch.getCanvasLocation();
		if (location == null)
		{
			return null;
		}

		assert AMOUNT_VARBITS.length == RUNE_VARBITS.length;

		graphics.setFont(FontManager.getRunescapeSmallFont());

		StringBuilder tooltipBuilder = new StringBuilder();
		for (int i = 0; i < AMOUNT_VARBITS.length; i++)
		{
			Varbits amountVarbit = AMOUNT_VARBITS[i];

			int amount = client.getSetting(amountVarbit);
			if (amount <= 0)
			{
				continue;
			}

			Varbits runeVarbit = RUNE_VARBITS[i];
			int runeId = client.getSetting(runeVarbit);

			graphics.setColor(Color.black);
			graphics.drawString("" + formatNumber(amount), location.getX() + (config.showIcons() ? 13 : 1),
				location.getY() + 14 + graphics.getFontMetrics().getHeight() * i);

			graphics.setColor(config.fontColor());
			graphics.drawString("" + formatNumber(amount), location.getX() + (config.showIcons() ? 12 : 0),
				location.getY() + 13 + graphics.getFontMetrics().getHeight() * i);

			tooltipBuilder
				.append(amount)
				.append(" <col=ffff00>")
				.append(runeImageCache.getName(runeId))
				.append("</col></br>");

			if (!config.showIcons())
			{
				continue;
			}

			BufferedImage runeImg = runeImageCache.getImage(runeId);
			if (runeImg != null)
			{
				OverlayUtil.renderImageLocation(graphics,
					new Point(location.getX(), location.getY() + 2 + (graphics.getFontMetrics().getHeight()) * i),
					runeImg);
			}
		}

		if (runePouch.getCanvasBounds().contains(client.getMouseCanvasPosition().getX(), client.getMouseCanvasPosition().getY()))
		{
			String tooltipText = tooltipBuilder.toString();
			Tooltip tooltip = new Tooltip(TooltipPriority.HIGH, tooltipText);
			tooltipRenderer.add(tooltip);
		}
		return null;
	}

	private static String formatNumber(int amount)
	{
		return amount < 1000 ? String.valueOf(amount) : amount / 1000 + "K";
	}
}
