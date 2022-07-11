package it.bz.odh.dcmeteoeurac;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import it.bz.idm.bdp.dto.DataMapDto;
import it.bz.idm.bdp.dto.DataTypeDto;
import it.bz.idm.bdp.dto.RecordDtoImpl;
import it.bz.idm.bdp.dto.SimpleRecordDto;
import it.bz.idm.bdp.dto.StationDto;
import it.bz.idm.bdp.dto.StationList;
import it.bz.odh.dcmeteoeurac.dto.ClimateDailyDto;
import it.bz.odh.dcmeteoeurac.dto.ClimatologyDto;
import it.bz.odh.dcmeteoeurac.dto.MetadataDto;

@Service
public class SyncScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(SyncScheduler.class);

    private static final String STATION_ID_PREFIX = "EURAC_";

    private static final String DATATYPE_ID_TMIN = "air-temperature-min";
    private static final String DATATYPE_ID_TMAX = "air-temperature-max";
    private static final String DATATYPE_ID_TMEAN = "air-temperature";
    private static final String DATATYPE_ID_PREC = "precipitation";

    @Value("${odh_client.period.climatology}")
    private Integer climatologyPeriod;

    @Value("${odh_client.period.climateDaily}")
    private Integer climateDailyPeriod;

    @Lazy
    @Autowired
    private OdhClient odhClient;

    @Lazy
    @Autowired
    private EuracClient euracClient;

    /**
     * Scheduled job A: sync stations and data types
     * 
     * @throws IOException
     */
    @Scheduled(cron = "${scheduler.job_stations}")
    public void syncJobStations() throws IOException {
        LOG.info("Cron job A started: Sync Stations with type {} and data types", odhClient.getIntegreenTypology());

        List<DataTypeDto> odhDataTypeList = new ArrayList<>();
        odhDataTypeList.add(new DataTypeDto(DATATYPE_ID_TMIN, "°C", "Minimum temperature", "min"));
        odhDataTypeList.add(new DataTypeDto(DATATYPE_ID_TMAX, "°C", "Maximum temperature", "max"));
        odhDataTypeList.add(new DataTypeDto(DATATYPE_ID_TMEAN, "°C", "Mean temperature", "mean"));
        odhDataTypeList.add(new DataTypeDto(DATATYPE_ID_PREC, "mm", "Precipitation", "total"));

        MetadataDto[] euracStations = euracClient.getStations();

        StationList odhStationList = new StationList();
        for (MetadataDto s : euracStations) {
            StationDto station = new StationDto(STATION_ID_PREFIX + s.getId(), s.getName(), s.getLat(), s.getLon());

            station.setOrigin(odhClient.getProvenance().getLineage());
            odhStationList.add(station);
        }

        try {
            odhClient.syncStations(odhStationList);
            odhClient.syncDataTypes(odhDataTypeList);
            LOG.info("Cron job for stations successful");
        } catch (WebClientRequestException e) {
            LOG.error("Cron job for stations failed: Request exception: {}", e.getMessage());
        }
    }

    /**
     * Scheduled job B: sync monthly climatologies
     * 
     * @throws IOException
     */
    @Scheduled(cron = "${scheduler.job_climatologies}")
    public void syncJobClimatologies() throws IOException {
        LOG.info("Cron job B started: Pushing climatology measurements for {}", odhClient.getIntegreenTypology());

        DataMapDto<RecordDtoImpl> rootMap = new DataMapDto<>();

        int prevStationId = -1;
        DataMapDto<RecordDtoImpl> stationMap = null;
        DataMapDto<RecordDtoImpl> tMinMetricMap = null;
        DataMapDto<RecordDtoImpl> tMaxMetricMap = null;
        DataMapDto<RecordDtoImpl> tMeanMetricMap = null;
        DataMapDto<RecordDtoImpl> precMetricMap = null;

        int timestampYear = Year.now().getValue() - 1;

        ClimatologyDto[] climatologies = euracClient.getClimatologies();

        for (ClimatologyDto climatology : climatologies) {
            if (prevStationId != climatology.getId()) {
                stationMap = rootMap.upsertBranch(STATION_ID_PREFIX + climatology.getId());

                tMinMetricMap = stationMap.upsertBranch(DATATYPE_ID_TMIN);
                tMaxMetricMap = stationMap.upsertBranch(DATATYPE_ID_TMAX);
                tMeanMetricMap = stationMap.upsertBranch(DATATYPE_ID_TMEAN);
                precMetricMap = stationMap.upsertBranch(DATATYPE_ID_PREC);

                prevStationId = climatology.getId();
            }

            int timestampMonth = climatology.getMonth();
            LocalDateTime localDateTime = LocalDateTime.of(timestampYear, timestampMonth, 1, 0, 0);
            long timestamp = localDateTime.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();

            addMeasurementToMap(tMinMetricMap, new SimpleRecordDto(timestamp, climatology.getTmin()),
                    climatologyPeriod);
            addMeasurementToMap(tMaxMetricMap, new SimpleRecordDto(timestamp, climatology.getTmax()),
                    climatologyPeriod);
            addMeasurementToMap(tMeanMetricMap, new SimpleRecordDto(timestamp, climatology.getTmean()),
                    climatologyPeriod);
            addMeasurementToMap(precMetricMap, new SimpleRecordDto(timestamp, climatology.getPrec()),
                    climatologyPeriod);
        }

        // Send the measurements to the Open Data Hub INBOUND API (writer)
        // WARNING: stations and datatypes must already exist, otherwise this call will
        // fail
        // It does not throw any exception, it will just not insert that data (this is a
        // known issue)
        // Exception will only be thrown on connection errors here! Please refer to the
        // writer log output or the database itself to see if data has been inserted
        try {
            odhClient.pushData(rootMap);
            LOG.info("Cron job for climatologies successful");
        } catch (WebClientRequestException e) {
            LOG.error("Cron job for climatologies failed: Request exception: {}", e.getMessage());
        }
    }

    /**
     * Scheduled job C: sync climate daily
     * 
     * @throws IOException
     */
    @Scheduled(cron = "${scheduler.job_climateDaily}")
    public void syncJobClimateDaily() throws IOException {
        LOG.info("Cron job C started: Pushing cliamte daily measurements for {}", odhClient.getIntegreenTypology());

        DataMapDto<RecordDtoImpl> rootMap = new DataMapDto<>();

        MetadataDto[] euracStations = euracClient.getStations();

        for (MetadataDto station : euracStations) {
            DataMapDto<RecordDtoImpl> stationMap = rootMap.upsertBranch(STATION_ID_PREFIX + station.getId());

            DataMapDto<RecordDtoImpl> tMinMetricMap = stationMap.upsertBranch(DATATYPE_ID_TMIN);
            DataMapDto<RecordDtoImpl> tMaxMetricMap = stationMap.upsertBranch(DATATYPE_ID_TMAX);
            DataMapDto<RecordDtoImpl> tMeanMetricMap = stationMap.upsertBranch(DATATYPE_ID_TMEAN);
            DataMapDto<RecordDtoImpl> precMetricMap = stationMap.upsertBranch(DATATYPE_ID_PREC);

            ClimateDailyDto[] climateDailies = euracClient.getClimateDaily(station.getId());

            for (ClimateDailyDto climateDaily : climateDailies) {

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                LocalDateTime localDateTime = LocalDate.parse(climateDaily.getDate(), formatter).atTime(12, 0);
                long timestamp = localDateTime.atZone(ZoneId.of("UTC")).toInstant().toEpochMilli();

                addMeasurementToMap(tMinMetricMap, new SimpleRecordDto(timestamp, climateDaily.getTmin()),
                        climateDailyPeriod);
                addMeasurementToMap(tMaxMetricMap, new SimpleRecordDto(timestamp, climateDaily.getTmax()),
                        climateDailyPeriod);
                addMeasurementToMap(tMeanMetricMap, new SimpleRecordDto(timestamp, climateDaily.getTmean()),
                        climateDailyPeriod);
                addMeasurementToMap(precMetricMap, new SimpleRecordDto(timestamp, climateDaily.getPrec()),
                        climateDailyPeriod);
            }
        }

        // Send the measurements to the Open Data Hub INBOUND API (writer)
        // WARNING: stations and datatypes must already exist, otherwise this call will
        // fail
        // It does not throw any exception, it will just not insert that data (this is a
        // known issue)
        // Exception will only be thrown on connection errors here! Please refer to the
        // writer log output or the database itself to see if data has been inserted
        try {
            odhClient.pushData(rootMap);
            LOG.info("Cron job for climate daily successful");
        } catch (WebClientRequestException e) {
            LOG.error("Cron job for climate daily failed: Request exception: {}", e.getMessage());
        }
    }

    private void addMeasurementToMap(DataMapDto<RecordDtoImpl> map, SimpleRecordDto measurement, int period) {
        if (map != null) {
            measurement.setPeriod(period);
            map.getData().add(measurement);
        }
    }
}
