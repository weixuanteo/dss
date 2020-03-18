/**
 * DSS - Digital Signature Services
 * Copyright (C) 2015 European Commission, provided under the CEF programme
 * 
 * This file is part of the "DSS - Digital Signature Services" project.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package eu.europa.esig.dss.pades;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import eu.europa.esig.dss.model.DSSException;
import eu.europa.esig.dss.validation.ByteRange;

public class ByteRangeTest {

	@Test
	public void validateByteRangeTest() {
		ByteRange byteRangeOk = new ByteRange(new int[] { 0, 1280, 2400, 480 });
		byteRangeOk.validate();

		Exception exception = assertThrows(DSSException.class, () -> {
			ByteRange byteRange = new ByteRange(new int[] { 1, 1280, 2400, 480 });
			byteRange.validate();
		});
		assertEquals("The ByteRange must cover start of file", exception.getMessage());
		exception = assertThrows(DSSException.class, () -> {
			ByteRange byteRange = new ByteRange(new int[] { 0, 0, 240, 480 });
			byteRange.validate();
		});
		assertEquals("The first hash part doesn't cover anything", exception.getMessage());
		exception = assertThrows(DSSException.class, () -> {
			ByteRange byteRange = new ByteRange(new int[] { 0, 1280, 240, 480 });
			byteRange.validate();
		});
		assertEquals("The second hash part must start after the first hash part", exception.getMessage());
		exception = assertThrows(DSSException.class, () -> {
			ByteRange byteRange = new ByteRange(new int[] { 0, 1280, 2400, 0 });
			byteRange.validate();
		});
		assertEquals("The second hash part doesn't cover anything", exception.getMessage());
		exception = assertThrows(DSSException.class, () -> {
			ByteRange byteRange = new ByteRange(new int[] { 0 });
			byteRange.validate();
		});
		assertEquals("Incorrect ByteRange size", exception.getMessage());
		exception = assertThrows(DSSException.class, () -> {
			ByteRange byteRange = new ByteRange(new int[0]);
			byteRange.validate();
		});
		assertEquals("Incorrect ByteRange size", exception.getMessage());
	}

}