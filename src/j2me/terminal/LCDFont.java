// LCDFont: Class for using tiny fonts with subpixel antialising
//
// Copyright 2005 Roar Lauritzsen <roarl@pvv.org>
//
// This class is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This class is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// The following link provides a copy of the GNU General Public License:
//     http://www.gnu.org/licenses/gpl.txt
// If you are unable to obtain the copy from this address, write to
// the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
// Boston, MA 02111-1307 USA

package terminal;

import java.io.IOException;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class LCDFont {
	//#ifdef midp2
	
	public final int fontWidth; // Width of one character

	public final int fontHeight; // Height of one character

	private final int imageWidth, imageHeight;

	private int[] bwBuf; // Black-white version of font

	private int[] colorBuf; // Colored version of font

	private int[] currentBuf;

	private long currentColor;

	private long[] cacheColor; // Color of each cached character

	private int FR, FG, FB, BR, BG, BB;

	// Create subpixel-antialiased font based on image resource name.
	// If BGR is set to true, subpixel order is reversed (normal is RGB)
	public LCDFont(String fontImageName, boolean BGR) {
		Image fontImage;
		try {
			fontImage = Image.createImage(fontImageName);
		} catch (IOException e) {
			throw new RuntimeException("Cannot load LCDFont: " + e);
		}
		imageWidth = fontImage.getWidth();
		imageHeight = fontImage.getHeight();
		fontWidth = imageWidth / 32;
		fontHeight = imageHeight / 3;
		bwBuf = new int[imageWidth * (imageHeight + 1)];
		colorBuf = new int[imageWidth * (imageHeight + 1)];
		/* Karl: add 1 to the imageHeight because some device seek beyond the bottom of the array when
		 * drawing.
		 */
		fontImage.getRGB(bwBuf, 0, imageWidth, 0, 0, imageWidth, imageHeight);
		if (BGR) {
			// For screens with BGR subpixel ordering, or for ROT_180
			for (int i = 0; i < bwBuf.length; i++) {
				int c = bwBuf[i];
				bwBuf[i] = ((c >> 16) & 0xff) + (c & 0xff00)
						+ ((c & 0xff) << 16);
			}
		}
		cacheColor = new long[96];
		currentBuf = bwBuf;
		currentColor = 0;
	}

	// Set the foreground and background color of font
	// For readability, input font colors will be modified
	public void setColor(int fg, int bg) {
		if (fg == 0xffffff && bg == 0) {
			currentBuf = bwBuf;
		} else if (((long) fg << 32) + bg != currentColor) {
			currentColor = ((long) fg << 32) + bg;
			currentBuf = colorBuf;

			// Because of the subpixel antialising, we cannot use the color
			// directly. Instead, we have to select a more "pastel" color,
			// and differentiate properly between background and foreground

			FR = (fg >> 16) & 0xff;
			FG = (fg >> 8) & 0xff;
			FB = fg & 0xff;

			BR = (bg >> 16) & 0xff;
			BG = (bg >> 8) & 0xff;
			BB = bg & 0xff;

			if (28 * FR + 55 * FG + 17 * FB >= 28 * BR + 55 * BG + 17 * BB) {
				// bright on dark
				FR = (6 * FR + 10 * 0xff) / 16; // Brighten foreground
				FG = (6 * FG + 10 * 0xff) / 16;
				FB = (6 * FB + 10 * 0xff) / 16;
				BR = (10 * BR) / 16; // Darken background
				BG = (10 * BG) / 16;
				BB = (10 * BB) / 16;
			} else {
				// dark on bright
				FR = (10 * FR) / 16; // Darken foreground
				FG = (10 * FG) / 16;
				FB = (10 * FB) / 16;
				BR = (6 * BR + 10 * 0xff) / 16; // Brighten background
				BG = (6 * BG + 10 * 0xff) / 16;
				BB = (6 * BB + 10 * 0xff) / 16;
			}
			// Scale to range 0-256 for later ">>8" instead of "/255"
			FR += FR >> 7;
			FG += FG >> 7;
			FB += FB >> 7;
			BR += BR >> 7;
			BG += BG >> 7;
			BB += BB >> 7;
		}
	}

	// Colored characters are cached in colorBuf and rendered as needed
	private void renderColorChar(int offset) {
		for (int y = 0; y < fontHeight; y++)
			for (int x = 0; x < fontWidth; x++) {
				int col = bwBuf[offset + y * imageWidth + x];
				int R = (col >> 16) & 0xff;
				int G = (col >> 8) & 0xff;
				int B = col & 0xff;

				R = (FR * R + BR * (255 - R)) >> 8;
				G = (FG * G + BG * (255 - G)) >> 8;
				B = (FB * B + BB * (255 - B)) >> 8;

				colorBuf[offset + y * imageWidth + x] = (R << 16) + (G << 8)
						+ B;
			}
	}

	// Draw one char
	public void drawChar(Graphics g, char c, int x, int y) {
		if (c <= ' ' || c > '~')
			return;
		c -= ' ';
		int offset = (c >> 5) * fontHeight * imageWidth + (c & 31) * fontWidth;
		if (currentBuf == colorBuf && cacheColor[c] != currentColor) {
			renderColorChar(offset);
			cacheColor[c] = currentColor;
		}
		g.drawRGB(currentBuf, offset, imageWidth, x, y, fontWidth, fontHeight,
				false);
	}
	//#endif
}