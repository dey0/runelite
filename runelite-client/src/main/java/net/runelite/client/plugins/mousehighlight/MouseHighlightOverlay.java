/*
 * Copyright (c) 2017, Aria <aria@ar1as.space>
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
package net.runelite.client.plugins.mousehighlight;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.annotation.Nullable;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayRenderer;
import net.runelite.client.ui.overlay.tooltips.Tooltip;
import net.runelite.client.ui.overlay.tooltips.TooltipPriority;
import net.runelite.client.ui.overlay.tooltips.TooltipRenderer;

class MouseHighlightOverlay extends Overlay
{
	// Grabs the colour and name from a target string
	// <col=ffffff>Player1
	private final MouseHighlightConfig config;
	private final Client client;
	private final TooltipRenderer tooltipRenderer;

	@Inject
	MouseHighlightOverlay(@Nullable Client client, MouseHighlightConfig config, OverlayRenderer overlayRenderer)
	{
		super(OverlayPosition.DYNAMIC);
		this.client = client;
		this.config = config;
		this.tooltipRenderer = overlayRenderer.getTooltipRenderer();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (client.getGameState() != GameState.LOGGED_IN || !config.enabled())
		{
			return null;
		}

		if (client.isMenuOpen())
		{
			return null;
		}

		MenuEntry[] menuEntries = client.getMenuEntries();
		int last = menuEntries.length - 1;

		if (last < 0)
		{
			return null;
		}

		MenuEntry menuEntry = menuEntries[last];
		String target = menuEntry.getTarget();
		String option = menuEntry.getOption();

		if (target.isEmpty())
		{
			return null;
		}

		// Trivial options that don't need to be highlighted, add more as they appear.
		switch (option)
		{
			case "Walk here":
			case "Cancel":
			case "Continue":
				return null;
		}

		Tooltip tooltip = new Tooltip(TooltipPriority.LOW,
			option + " " + target);
		tooltipRenderer.add(tooltip);

		return null;
	}
}
