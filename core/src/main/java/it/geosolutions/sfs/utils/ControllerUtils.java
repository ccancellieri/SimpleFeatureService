/*
 *  SFS - Open Source Simple Feature Service implementation
 *  Copyright (C) 2007-2012 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.sfs.utils;

import org.apache.commons.collections.map.CaseInsensitiveMap;

/**
 * @author Carlo Cancellieri - ccancellieri@hotmail.com
 */
public abstract class ControllerUtils {

	/**
	 * @param hints String containing a map providing implementation specific hints. The expected format is 
		key1:value1;key2:value2;...
	 * @return the obtained map or null
	 */
	public static CaseInsensitiveMap parseHints(String hints){
		if (hints==null)
			return null;
		
		CaseInsensitiveMap _hints=new CaseInsensitiveMap();
		
		String[] entryes=hints.split(";");
		for (String entry: entryes){
			String[] keyVal=entry.split(":");
			if (keyVal.length!=2)
				continue;
			_hints.put(keyVal[0], keyVal[1]);
		}
		return _hints;
	}

}
