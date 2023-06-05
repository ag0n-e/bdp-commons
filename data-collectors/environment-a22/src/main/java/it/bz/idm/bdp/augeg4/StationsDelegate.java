// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp.augeg4;

import it.bz.idm.bdp.augeg4.dto.tohub.AugeG4ProcessedDataToHubDto;
import it.bz.idm.bdp.augeg4.dto.tohub.StationId;
import it.bz.idm.bdp.augeg4.fun.convert.tohub.StationMapping;
import it.bz.idm.bdp.augeg4.fun.convert.tohub.StationMappings;
import it.bz.idm.bdp.dto.StationDto;
import it.bz.idm.bdp.dto.StationList;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static it.bz.idm.bdp.augeg4.fun.convert.tohub.DataConverterHub.PREFIX;

class StationsDelegate {

    private static final Logger LOG = LoggerFactory.getLogger(StationsDelegate.class.getName());

    private final DataService dataService;
    private Map<StationId, StationDto> stationsMap = new ConcurrentHashMap<>();
    private final StationMappings stationMappings = new StationMappings();


    StationsDelegate(DataService dataService) {
        this.dataService = dataService;
    }


    void loadPreviouslySyncedStations() throws Exception {
        LOG.debug("loadPreviouslySyncedStations() called");
        try {
            StationList stations = dataService.getDataPusherHub().getSyncedStations();
            stations.forEach(this::insertStationInMapIfNew);
        } catch (Exception e) {
            LOG.error("Load of previously synced stations failed: {}.", e.getMessage());
            throw e;
        }
    }


    private void insertStationInMapIfNew(StationDto station) {
        Optional<StationId> stationIdContainer = StationId.fromValue(station.getId(), PREFIX);
        if(!stationIdContainer.isPresent()) {
            LOG.warn("insertStationInMapIfNew() called with a StationDto that uses a different prefix");
            return;
        }
        StationId stationId = stationIdContainer.get();
        if (!stationsMap.containsKey(stationId)) {
            stationsMap.put(stationId, station);
        }
    }


    void syncStationsWithHub() {
        LOG.debug("syncStations() called");
        try {
            StationList stationList = dequeueStations();
            dataService.getDataPusherHub().syncStations(stationList);
        } catch (Exception e) {
            LOG.error("Sync of stations failed: {}.", e.getMessage());
            throw e;
        }
    }


    private StationList dequeueStations() {
        Map<StationId, StationDto> oldStationsMap = this.stationsMap;
        return new StationList(oldStationsMap.values());
    }


    void prepareStationsForHub(List<AugeG4ProcessedDataToHubDto> data) {
        LOG.debug("prepareStationsForHub() called. Try to insert a new station if new. Size ["+data.size()+"]");
        data.forEach(this::insertStationFromConvertedData);
    }


    private void insertStationFromConvertedData(AugeG4ProcessedDataToHubDto dto) {
        StationId id = dto.getStationId();
        LOG.debug("insertStationFromConvertedData stationsMap size ["+stationsMap.size()+"]");
        LOG.debug("insertStationFromConvertedData id ["+id.toString()+"]");
        if (!stationsMap.containsKey(id)) {
            insertStationFromConvertedData(dto, id);
        }
    }


    private void insertStationFromConvertedData(AugeG4ProcessedDataToHubDto dto, StationId id) {
        LOG.debug("insertStationFromConvertedData dto ["+dto.toString()+"] id ["+id.toString()+"]");
        Optional<StationDto> station = createStationDtoFromConvertedData(dto);
        station.ifPresent(stationDto -> stationsMap.put(id, stationDto));
    }


    private Optional<StationDto> createStationDtoFromConvertedData(AugeG4ProcessedDataToHubDto dto) {
        LOG.debug("createStationDtoFromConvertedData dto ["+dto.toString()+"] ");
        return stationMappings.getMapping(dto.getStationId().getControlUnitId())
                .map(mapping -> mapStationMappingToStationDto(dto, mapping));
    }


    private StationDto mapStationMappingToStationDto(AugeG4ProcessedDataToHubDto dto, StationMapping stationMapping) {
        LOG.debug("mapStationMappingToStationDto dto ["+dto.toString()+"] ");
        StationDto station = new StationDto(
                dto.getStationId().getValue(),
                stationMapping.getName(),
                stationMapping.getLatitude(),
                stationMapping.getLongitude()
        );
        station.setOrigin(dataService.getDataPusherHub().getOrigin());
        station.setStationType(dataService.getDataPusherHub().getStationType());
        LOG.debug("mapStationMappingToStationDto station ["+station.toString()+"] ");
        return station;
    }

    public int getStationsCount() {
        return stationsMap.size();
    }
}
