package it.bz.odh.trafficprovbz;

import com.jayway.jsonpath.JsonPath;
import it.bz.idm.bdp.dto.*;
import it.bz.odh.trafficprovbz.dto.AggregatedDataDto;
import it.bz.odh.trafficprovbz.dto.ClassificationSchemaDto;
import it.bz.odh.trafficprovbz.dto.MetadataDto;
import it.bz.odh.trafficprovbz.dto.PassagesDataDto;
import net.minidev.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class SyncScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(SyncScheduler.class);

	private static final String STATION_TYPE = "TrafficSensor";

	//TODO: Ask PO of NOI if I should use a datatype for each value or one general and with json
	private static final String DATATYPE_ID_TRAFFIC = "type";
	private static final String DATATYPE_ID_BlUETOOTH = "vehicle detection";

	@Value("${odh_client.period}")
	private Integer period;

	private final OdhClient odhClient;

	private final FamasClient famasClient;

	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

	private Date startPeriodTraffic = new Date(System.currentTimeMillis() - 300 * 1000);

	private Date startPeriodBluetooth = new Date(System.currentTimeMillis() - 300 * 1000);

	public SyncScheduler(@Lazy OdhClient odhClient, @Lazy FamasClient famasClient) {
		this.odhClient = odhClient;
		this.famasClient = famasClient;
	}

	/**
	 * Scheduled job stations: Sync stations and data types
	 */
	@Scheduled(cron = "${scheduler.job_stations}")
	public void syncJobStations() {
		LOG.info("Cron job stations started: Sync Stations with type {} and data types", odhClient.getIntegreenTypology());
		try {
			List<DataTypeDto> odhDataTypeList = new ArrayList<>();
			odhDataTypeList.add(new DataTypeDto(DATATYPE_ID_TRAFFIC, null, "Data of traffic", "string"));
			odhDataTypeList.add(new DataTypeDto(DATATYPE_ID_BlUETOOTH, null, "Detects the passed vehicles", "string"));

			ClassificationSchemaDto[] classificationDtos;
			classificationDtos = famasClient.getClassificationSchemas();
			ArrayList<LinkedHashMap<String, String>> odhClassesList = new ArrayList<>();

			for (ClassificationSchemaDto c : classificationDtos) {
				ArrayList<LinkedHashMap<String, String>> classes = JsonPath.read(c.getOtherFields(), "$.Classi");
				odhClassesList.addAll(classes);
			}


			MetadataDto[] metadataDtos = famasClient.getStationsData();
			StationList odhStationList = new StationList();

			for (MetadataDto metadataDto : metadataDtos) {
				JSONObject otherFields = new JSONObject(metadataDto.getOtherFields());
				Double lat = JsonPath.read(otherFields, "$.GeoInfo.Latitudine");
				Double lon = JsonPath.read(otherFields, "$.GeoInfo.Longitudine");

				LinkedHashMap<String, String> classificationSchema = getClassificationSchema(odhClassesList, metadataDto);
				metadataDto.setOtherField("SchemaDiClassificazione", classificationSchema);

				ArrayList<LinkedHashMap<String, String>> lanes = JsonPath.read(otherFields, "$.CorsieInfo");

				for (LinkedHashMap<String, String> lane : lanes) {
					String description = JsonPath.read(lane, "$.Descrizione");
					String stationName = metadataDto.getName() + ":" + description;
					StationDto station = new StationDto(metadataDto.getId(), stationName, lat, lon);
					station.setOrigin(odhClient.getProvenance().getLineage());
					station.setStationType(STATION_TYPE);
					station.setMetaData(metadataDto.getOtherFields());

					odhStationList.add(station);
				}
				LOG.info(odhStationList.toString());
			}
			odhClient.syncStations(odhStationList);
			odhClient.syncDataTypes(odhDataTypeList);
			LOG.info("Cron job stations successful");
		} catch (Exception e) {
			LOG.error("Cron job stations failed: Request exception: {}", e.getMessage());
		}
	}

	/**
	 * Scheduled job traffic measurements: Example on how to send measurements
	 */
	@Scheduled(cron = "${scheduler.job_measurements}")
	public void syncJobTrafficMeasurements() {
		LOG.info("Cron job measurements started: Pushing measurements for {}", odhClient.getIntegreenTypology());
		try {
			DataMapDto<RecordDtoImpl> rootMap = new DataMapDto<>();

			AggregatedDataDto[] aggregatedDataDtos = famasClient.getAggregatedDataOnStations(sdf.format(startPeriodTraffic), sdf.format(new Date()));

			for (AggregatedDataDto aggregatedDataDto : aggregatedDataDtos) {

				mapAggregatedDataToRoot(rootMap, aggregatedDataDto);
			}
			odhClient.pushData(rootMap);
			LOG.info("Cron job traffic measurements successful");
			startPeriodTraffic = new Date();
		} catch (Exception e) {
			LOG.error("Cron job traffic measurements failed: Request exception: {}", e.getMessage());
		}
	}

	/**
	 * Scheduled job bluetooth measurements: sync climate daily
	 */
	@Scheduled(cron = "${scheduler.job_measurements}")
	public void syncJobBluetoothMeasurements() {
		LOG.info("Cron job measurements started: Pushing bluetooth measurements for {}", odhClient.getIntegreenTypology());
		try {
			MetadataDto[] metadataDtos = famasClient.getStationsData();

			for (MetadataDto metadataDto : metadataDtos) {
				DataMapDto<RecordDtoImpl> rootMap = new DataMapDto<>();

				DataMapDto<RecordDtoImpl> stationMap = rootMap.upsertBranch(metadataDto.getId());

				DataMapDto<RecordDtoImpl> bluetoothMetricMap = stationMap.upsertBranch(DATATYPE_ID_BlUETOOTH);

				PassagesDataDto[] passagesDataDtos = famasClient.getPassagesDataOnStations(metadataDto.getId(), sdf.format(startPeriodBluetooth), sdf.format(new Date()));

				for (PassagesDataDto passagesDataDto : passagesDataDtos) {
					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
					Long timestamp = formatter.parse(passagesDataDto.getDate()).getTime();
					SimpleRecordDto measurement = new SimpleRecordDto(timestamp, passagesDataDto.getIdVehicle(), period);
					bluetoothMetricMap.getData().add(measurement);
				}

				try {
					// Push data for every station separately to avoid out of memory errors
					odhClient.pushData(rootMap);
					LOG.info("Push data for station {} bluetooth measurement successful", metadataDto.getName());
				} catch (WebClientRequestException e) {
					LOG.error("Push data for station {} bluetooth measurement failed: Request exception: {}", metadataDto.getName(),
						e.getMessage());
				}
			}
			LOG.info("Cron job for bluetooth measurements successful");
			startPeriodBluetooth = new Date();
		} catch (Exception e) {
			LOG.error("Push data for bluetooth measurements failed: Request exception: {}", e.getMessage());
		}
	}

	public LinkedHashMap<String, String> getClassificationSchema(ArrayList<LinkedHashMap<String, String>> odhClassesList, MetadataDto s) {
		for (LinkedHashMap<String, String> odhClass : odhClassesList) {
			int code = JsonPath.read(odhClass, "$.Codice");
			if (code == s.getClassificationSchema()) {
				return odhClass;
			}
		}
		return null;
	}

	private void mapAggregatedDataToRoot(DataMapDto<RecordDtoImpl> rootMap, AggregatedDataDto aggregatedDataDto) throws ParseException {
		DataMapDto<RecordDtoImpl> stationMap = rootMap.upsertBranch(aggregatedDataDto.getId());
		DataMapDto<RecordDtoImpl> metricMap = stationMap.upsertBranch(DATATYPE_ID_TRAFFIC);
		Map<String, Object> aggregatedDataMap = new HashMap<>();
		aggregatedDataMap.put("created_on", new Date().getTime());
		aggregatedDataMap.put("total-transits", aggregatedDataDto.getTotalTransits());
		JSONObject otherFields = new JSONObject(aggregatedDataDto.getOtherFields());
		if (otherFields.containsKey("TotaliPerClasseVeicolare")) {
			LinkedHashMap<String, Integer> classes = JsonPath.read(otherFields, "$.TotaliPerClasseVeicolare");
			//Set<String> keys = classes.keySet();
			for (Map.Entry<String, Integer> entry : classes.entrySet()) {
				switch (entry.getKey()) {
					case "1":
						aggregatedDataMap.put("number-of-motorcycles", entry.getValue());
						break;
					case "2":
						aggregatedDataMap.put("number-of-cars", entry.getValue());
						break;
					case "3":
						aggregatedDataMap.put("number-of-cars-and-minivans-with-trailer", entry.getValue());
						break;
					case "4":
						aggregatedDataMap.put("number-of-small-trucks-and-vans", entry.getValue());
						break;
					case "5":
						aggregatedDataMap.put("number-of-medium-sized-trucks", entry.getValue());
						break;
					case "6":
						aggregatedDataMap.put("number-of-big-trucks", entry.getValue());
						break;
					case "7":
						aggregatedDataMap.put("number-of-articulated-trucks", entry.getValue());
						break;
					case "8":
						aggregatedDataMap.put("number-of-articulated-lorries", entry.getValue());
						break;
					case "9":
						aggregatedDataMap.put("number-of-busses", entry.getValue());
						break;
					case "10":
						aggregatedDataMap.put("number-of-unclassified-vehicles", entry.getValue());
						break;
				}
			}
			aggregatedDataMap.put("average-vehicle-speed", aggregatedDataDto.getAverageVehicleSpeed());
			aggregatedDataMap.put("headway", aggregatedDataDto.getHeadway());
			aggregatedDataMap.put("headway-variance", aggregatedDataDto.getHeadwayVariance());
			aggregatedDataMap.put("gap", aggregatedDataDto.getGap());
			aggregatedDataMap.put("gap-variance", aggregatedDataDto.getGapVariance());
		}

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		Long timestamp = formatter.parse(aggregatedDataDto.getDate()).getTime();
		SimpleRecordDto measurement = new SimpleRecordDto(timestamp, aggregatedDataMap, period);
		metricMap.getData().add(measurement);
	}

}
