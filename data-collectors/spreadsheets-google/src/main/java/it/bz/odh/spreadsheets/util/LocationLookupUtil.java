// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.odh.spreadsheets.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import it.bz.idm.bdp.dto.StationDto;
import it.bz.idm.bdp.util.LocationLookup;
import it.bz.idm.bdp.util.NominatimException;
import it.bz.idm.bdp.util.NominatimLocationLookupUtil;

@Component
public class LocationLookupUtil {
    
    private LocationLookup lookUpUtil = new NominatimLocationLookupUtil();
    
    @Value("${headers_addressId:#{null}}")
    private String addressId;    
    /**
     * Uses nominatim to guess the coordinates by a address
     * @param dto to guess the position off
     */
    public void guessPositionByAddress(StationDto dto) {
        Object addressObject = dto.getMetaData().get(addressId);
        if (addressObject != null && !addressObject.toString().isEmpty()) {
            Double[] coordinates;
			try {
				coordinates = lookUpUtil.lookupCoordinates(addressObject.toString());
				if (coordinates[0] != null && coordinates[1] != null) {
	                dto.setLongitude(coordinates[0]);
	                dto.setLatitude(coordinates[1]);
	            }
			} catch (NominatimException e) {
				e.printStackTrace();
			}
        }
    }

}
