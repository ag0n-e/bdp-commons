// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp.augeg4.face;

import it.bz.idm.bdp.augeg4.dto.tohub.AugeG4ProcessedDataToHubDto;
import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.DataTypeDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;

import java.util.List;

/**
 * Maps a list of {@link AugeG4ProcessedDataToHubDto} to a single {@link DataMapDto}, used by the Pusher to send data to the hub
 */
public interface DataPusherMapperFace {

    /**
     * Maps the list of DTOs to the hierarchical structure needed by the Pusher.
     *
     * Example of generated map:
     * <pre>
     * rootMap --
     *          |-- Station: "A"
     *            |-- Parameter: "temperature"
     *              |-- Values: { (time, 23), (time, ...) }
     *            |-- Parameter: "pressure"
     *              |-- Values: { (time, 1.2), (time, ...) }
     *          |-- Station: "B"
     *            |-- Parameter: "temperature"
     *              |-- Values: { (time, 21), (time, ...) }
     *            |-- Parameter: "pressure"
     *              |-- Values: { (time, 1.5), (time, ...) }
     * </pre>
     * where values are a list of SimpleRecordDtos
     *
     * @param measurementsByStations List of DTOs to map
     * @return Hierarchical map structure
     */
    DataMapDto<RecordDtoImpl> mapData(List<AugeG4ProcessedDataToHubDto> measurementsByStations);

    /**
     * Maps every DataTypeDto to two DataTypeDtos: one for the raw measurement and one for the processed measurement.
     * For example:
     * A DataTypeDto with the name 'temperature' will be mapped to two DataTypeDto with the names 'temperature_raw'
     * and 'temperature_processed'.
     * @param dataTypeDtoList
     * @return
     */
    List<DataTypeDto> mapDataTypes(List<DataTypeDto> dataTypeDtoList);

}
