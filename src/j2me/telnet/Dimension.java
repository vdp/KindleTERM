package telnet;

/* This file is part of "Telnet Floyd".
 *
 * (c) Radek Polak 2003-2004. All Rights Reserved.
 *
 * Please visit project homepage at http://phoenix.inf.upol.cz/~polakr
 *
 * --LICENSE NOTICE--
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * --LICENSE NOTICE--
 *
 */

/**
 * Class used instead of missing java.awt.Dimension.
 */

public class Dimension {

	public int width, height;

	public Dimension() {
	}

	public Dimension( int width, int height ) {
		this.width = width;
		this.height = height;
	}

}