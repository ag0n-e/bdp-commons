// SPDX-FileCopyrightText: NOI Techpark <digital@noi.bz.it>
//
// SPDX-License-Identifier: AGPL-3.0-or-later

package it.bz.idm.bdp.dcemobilityh2;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import it.bz.idm.bdp.dcemobilityh2.dto.ChargingPointsDtoV2;
import it.bz.idm.bdp.dcemobilityh2.dto.HydrogenDto;
import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.DataTypeDto;
import it.bz.idm.bdp.dto.ProvenanceDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.dto.SimpleRecordDto;
import it.bz.idm.bdp.dto.StationDto;
import it.bz.idm.bdp.dto.StationList;
import it.bz.idm.bdp.json.NonBlockingJSONPusher;

@Service
public class HydrogenDataPusher extends NonBlockingJSONPusher {

    private static final Logger LOG = LoggerFactory.getLogger(HydrogenDataPusher.class.getName());

    @Autowired
    private Environment env;

    public HydrogenDataPusher() {
        LOG.debug("START.constructor.");
        LOG.debug("END.constructor.");
    }

    @Override
    public String initIntegreenTypology() {
        String stationType = "EChargingStation";
        return stationType;
    }

    @Override
    public <T> DataMapDto<RecordDtoImpl> mapData(T rawData) {
        LOG.debug("START.mapData");

        @SuppressWarnings("unchecked")
        List<HydrogenDto> data = (List<HydrogenDto>) rawData;
        if (data == null) {
            return null;
        }

        DataMapDto<RecordDtoImpl> map = new DataMapDto<>();
        Date now = new Date();
        String availableValue = env.getRequiredProperty(HydrogenDataConverter.PLUG_AVAILABLE_KEY);
        Integer period = env.getProperty(HydrogenDataConverter.PERIOD_KEY, Integer.class);

        for(HydrogenDto dto: data){
            DataMapDto<RecordDtoImpl> recordsByType = new DataMapDto<RecordDtoImpl>();
            Integer availableStations=0;
            StationDto stationDto = dto.getStation();
            List<ChargingPointsDtoV2> pointList = dto.getPointList();
            for (ChargingPointsDtoV2 point : pointList){
                List<RecordDtoImpl> records = new ArrayList<RecordDtoImpl>();
                if (availableValue.equals(point.getState())) {
                    availableStations++;
                }
                SimpleRecordDto record = new SimpleRecordDto(now.getTime(), availableStations.doubleValue());
                record.setPeriod(period);
                records.add(record);
                DataMapDto<RecordDtoImpl> dataSet = new DataMapDto<>(records);
                recordsByType.getBranch().put(DataTypeDto.NUMBER_AVAILABE, dataSet);
            }
            map.getBranch().put(stationDto.getId(), recordsByType);
        }

        LOG.debug("map: "+map);
        LOG.debug("END.mapData");
        return map;
    }

    public DataMapDto<RecordDtoImpl> mapPlugData2Bdp(List<HydrogenDto> data) {
        LOG.debug("START.mapPlugData2Bdp");

        if (data == null) {
            return null;
        }

        DataMapDto<RecordDtoImpl> map = new DataMapDto<>();
        Date now = new Date();
        String availableValue = env.getRequiredProperty(HydrogenDataConverter.PLUG_AVAILABLE_KEY);
        Integer period = env.getProperty(HydrogenDataConverter.PERIOD_KEY, Integer.class);

        for(HydrogenDto dto: data) {
            List<ChargingPointsDtoV2> pointList = dto.getPointList();
            for (ChargingPointsDtoV2 point : pointList){
                DataMapDto<RecordDtoImpl> recordsByType = new DataMapDto<RecordDtoImpl>();
                List<RecordDtoImpl> records = new ArrayList<RecordDtoImpl>();
                SimpleRecordDto record = new SimpleRecordDto();
                record.setTimestamp(now.getTime());
                record.setValue(availableValue.equals(point.getState()) ? 1. : 0);
                record.setPeriod(period);
                records.add(record);
                recordsByType.getBranch().put("echarging-plug-status", new DataMapDto<RecordDtoImpl>(records));
                //String id = dto.getId()+"-"+point.getOutlets().get(0).getId();
                String id = point.getId();
                map.getBranch().put(id, recordsByType);
            }
        }

        LOG.debug("map: "+map);
        LOG.debug("END.mapPlugData2Bdp");
        return map;
    }

    public StationList mapStations2Bdp(List<HydrogenDto> data) {
        LOG.debug("START.mapStations2Bdp");
        if (data == null) {
            return null;
        }

        StationList stations = new StationList();
        for (HydrogenDto dto : data) {
            StationDto stationDto = dto.getStation();
            stations.add(stationDto);
        }
        LOG.debug("stations: "+stations);
        LOG.debug("END.mapStations2Bdp");
        return stations;
    }

    public StationList mapPlugs2Bdp(List<HydrogenDto> data) {
        LOG.debug("START.mapPlugs2Bdp");
        if (data == null) {
            return null;
        }

        StationList plugs = new StationList();
        for (HydrogenDto dto : data) {
            List<StationDto> plugList = dto.getPlugList();
            plugs.addAll(plugList);
        }

        LOG.debug("plugs: "+plugs);
        LOG.debug("END.mapPlugs2Bdp");
        return plugs;
    }

    @Override
    public String toString() {
        String str1 = "http://" + config.getString(HOST_KEY) + ":" + config.getString(PORT_KEY) + config.getString("json_endpoint");
        String str2 =
                "integreenTypology=" + this.integreenTypology   + "  " +
                "DEFAULT_HOST="      + DEFAULT_HOST     + "  " +
                "DEFAULT_PORT="      + DEFAULT_PORT     + "  " +
                "DEFAULT_ENDPOINT="  + DEFAULT_ENDPOINT + "  " +
                "";
        return str2 + " ---> " + str1;
    }

	@Override
	public ProvenanceDto defineProvenance() {
		return new ProvenanceDto(null,env.getProperty("provenance_name"), env.getProperty("provenance_version"), env.getProperty("app.origin"));
	}
}
